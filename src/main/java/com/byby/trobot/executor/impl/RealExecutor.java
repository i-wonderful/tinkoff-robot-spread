package com.byby.trobot.executor.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.AppProperties;
import com.byby.trobot.controller.exception.UserDataException;
import com.byby.trobot.controller.handler.ExceptionHandler;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.mapper.PortfolioMapper;
import com.byby.trobot.executor.Executor;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OperationsService;
import ru.tinkoff.piapi.core.OrdersService;
import ru.tinkoff.piapi.core.UsersService;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.stream.OrdersStreamService;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.byby.trobot.executor.impl.Helper.*;
import static ru.tinkoff.piapi.core.utils.MapperUtils.*;

/**
 * Работа с реальным счетом
 */
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "false")
@ApplicationScoped
public class RealExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(RealExecutor.class);
    private static final int QUANTITY_DEFAULT = 1;

    private UsersService usersService;
    private OperationsService operationsService;
    private PortfolioMapper portfolioMapper;
    private OrdersService ordersService;
    private OrdersStreamService ordersStreamService;

    private EventLogger eventLogger;
    private AppProperties appProperties;
    private ExceptionHandler exceptionHandler;

    private String accountId;

    public RealExecutor(InvestApi api, EventLogger eventLogger, PortfolioMapper portfolioMapper, AppProperties appProperties, ExceptionHandler exceptionHandler) {
        log.info(">>> Init Real Executor");
        this.usersService = api.getUserService();
        this.operationsService = api.getOperationsService();
        this.eventLogger = eventLogger;
        this.appProperties = appProperties;
        this.portfolioMapper = portfolioMapper;
        this.ordersService = api.getOrdersService();
        this.ordersStreamService = api.getOrdersStreamService();
        this.exceptionHandler = exceptionHandler;
    }

    public Uni<String> loadAccountId() {
        if (accountId != null) {
            return Uni.createFrom().item(accountId);
        }
        Uni<String> accountIdUni =
                Uni.createFrom()
                        .completionStage(usersService.getAccounts())
                        .onItem()
                        .transform(accounts -> findOpenAccountId(accounts).orElse(null));

        accountIdUni.subscribe()
                .with(Unchecked.consumer(accountId -> {
                    if (accountId == null) {
                        throw new UserDataException("Не найден аккаунт пользователя!");
                    }
                    log.info(">>> Load Account id: " + accountId);
                    this.accountId = accountId;
                }));

        return accountIdUni;
    }

    @Override
    public Uni<PortfolioDto> getPortfolio() {
        return loadAccountId()
                .onItem()
                .transformToUni(accountId ->
                        getPortfolio(accountId)
                                .onItem()
                                .transform(portfolio -> portfolioMapper.toDto(portfolio, accountId)));
    }

    private Uni<Portfolio> getPortfolio(String accountId) {
        return toUni(operationsService.getPortfolio(accountId));
    }

    @Override
    public Uni<PostOrderResponse> postBuyLimitOrder(String figi, BigDecimal price) {
        return toUni(ordersService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_BUY,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    @Override
    public Uni<PostOrderResponse> postSellLimitOrder(String figi, BigDecimal price) {
        if (appProperties.isMarginAllow()) {
            return postSellLimitOrderDirect(figi, price);
        } else {
            return hasPosition(figi)
                    .onItem()
                    .transformToUni(isHasPosition -> {
                        if (isHasPosition) {
                            System.out.println(">>> Post sell 1");
                            return postSellLimitOrderDirect(figi, price);
                        } else {
                            eventLogger.log("Маржинальная торговля запрещена и нет позиций в этой акции. Продажу не выставляем");
                            return Uni.createFrom().nothing();
                        }
                    });
        }
    }

    private Uni<PostOrderResponse> postSellLimitOrderDirect(String figi, BigDecimal price) {
        return toUni(ordersService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_SELL,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    public Uni<Boolean> hasPosition(String figi) {
        return toUni(operationsService.getPositions(this.accountId))
                .onItem()
                .transform(positions ->
                        Optional.ofNullable(positions.getSecurities()).orElse(Collections.emptyList())
                        .stream()
                        .anyMatch(securityPosition -> figi.equals(securityPosition.getFigi())));
    }

    // todo пересчитать оптимальность по двум заявкам из стакана
    @Override
    public boolean isMyBuyOrderOptimal(OrderState myOrderBuy, Order askFromOrderbook) {
        if (askFromOrderbook == null) {
            log.warn(">>> Bid form orderbook is null.");
            return true;
        }
        MoneyValue myPrice = myOrderBuy.getInitialSecurityPrice();
        Quotation orderbookPrice = askFromOrderbook.getPrice();
        return isEqual(myPrice, orderbookPrice);
    }

    @Override
    public boolean isMySellOrderOptimal(OrderState myOrderSell, Order bidFromOrderbook) {
        if (bidFromOrderbook == null) {
            log.warn(">>> Ask form orderbook is null.");
            return true;
        }
        MoneyValue myPrice = myOrderSell.getInitialSecurityPrice();
        Quotation orderBookPrice = bidFromOrderbook.getPrice();
        return isEqual(myPrice, orderBookPrice);
    }

    /**
     * Отменить заявку.
     */
    @Override
    public Uni<Instant> cancelOrder(String orderId) {
        log.info(">>>> cancelOrder " + this.accountId, orderId);
        return toUni(ordersService.cancelOrder(this.accountId, orderId))
                .invoke(() -> eventLogger.log("Отменена заявка orderId=" + orderId))
                .onFailure()
                .invoke(throwable -> exceptionHandler.handle(throwable));
    }

    /**
     * Отменить все активные заявки.
     *
     * @return
     */
    @Override
    public Uni<Void> cancelAllOrders() {
        return getMyOrders()
                .onItem()
                .transformToUni(orderStates -> orderStates.isEmpty() ?
                        Uni.createFrom().voidItem() :
                        Uni.combine().all().unis(
                                orderStates.stream().map(o -> cancelOrder(o.getOrderId())).collect(Collectors.toList())
                        ).discardItems()
                );
    }


    @Override
    public Uni<List<OrderState>> getMyOrders() {
        return toUni(ordersService.getOrders(this.accountId));
    }
}
