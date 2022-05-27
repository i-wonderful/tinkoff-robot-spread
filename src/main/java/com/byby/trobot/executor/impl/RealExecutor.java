package com.byby.trobot.executor.impl;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.RobotProperties;
import com.byby.trobot.controller.dto.PortfolioDto;
import com.byby.trobot.controller.dto.mapper.PortfolioMapper;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.controller.handler.ExceptionHandler;
import com.byby.trobot.executor.Executor;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OperationsService;
import ru.tinkoff.piapi.core.OrdersService;
import ru.tinkoff.piapi.core.UsersService;
import ru.tinkoff.piapi.core.models.Portfolio;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.byby.trobot.executor.impl.Helper.*;
import static ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation;

/**
 * Операции с реальным счетом
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

    private EventLogger eventLogger;
    private RobotProperties properties;
    private ExceptionHandler exceptionHandler;
    private AppCache appCache;
    private String accountId;

    public RealExecutor(InvestApi api, EventLogger eventLogger, PortfolioMapper portfolioMapper, RobotProperties properties, ExceptionHandler exceptionHandler, AppCache appCache) {
        log.info(">>> Init RealExecutor");
        this.usersService = api.getUserService();
        this.operationsService = api.getOperationsService();
        this.eventLogger = eventLogger;
        this.properties = properties;
        this.portfolioMapper = portfolioMapper;
        this.ordersService = api.getOrdersService();
        this.exceptionHandler = exceptionHandler;
        this.appCache = appCache;
    }

    @Override
    public Uni<String> loadAccountId() {
        if (accountId != null) {
            return Uni.createFrom().item(accountId);
        }
        log.info(">>> loadAccountId");
        return toUni(usersService.getAccounts())
                .onItem()
                .transform(accounts -> {
                            accountId = findOpenAccountId(accounts)
                                    .orElseThrow(() -> new CriticalException("Не найден активный аккаунт! Проверьте их наличие."));
                            log.info(">>> AccountId=" + this.accountId);
                            appCache.putAccountId(accountId);
                            return accountId;
                        }
                )
                .onFailure()
                .transform(throwable -> new CriticalException(throwable, "Не удалось получить список аккаунтов! Проверьте токен и доступ."))
                ;
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

    /**
     * Выставить лимитную заявку на покупку.
     *
     * @param figi
     * @param price
     * @return
     */
    @Override
    public Uni<PostOrderResponse> postBuyLimitOrder(String figi, BigDecimal price) {
        return toUni(ordersService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_BUY,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()))
                .onFailure()
                .recoverWithUni(throwable -> {
                    exceptionHandler.handle(throwable, "Ошибка выставления заявки на покупку.", figi);
                    return Uni.createFrom().nothing();
                });
    }

    @Override
    public Uni<PostOrderResponse> postSellLimitOrder(String figi, BigDecimal price) {
        if (properties.isMarginAllow()) {
            return postSellLimitOrderDirect(figi, price);
        } else {
            return hasPosition(figi)
                    .onItem()
                    .transformToUni(isHasPosition -> {
                        if (isHasPosition) {
                            return postSellLimitOrderDirect(figi, price);
                        } else {
                            eventLogger.log("Маржинальная торговля запрещена и нет позиций в этой акции. Продажу не выставляем.", figi);
                            return Uni.createFrom().nothing();
                        }
                    });
        }
    }

    /**
     * Выставить лимитную заявку на продажу.
     *
     * @param figi
     * @param price
     * @return
     */
    private Uni<PostOrderResponse> postSellLimitOrderDirect(String figi, BigDecimal price) {
        return toUni(ordersService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_SELL,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()))
                .onFailure()
                .recoverWithUni(throwable -> {
                    exceptionHandler.handle(throwable, "Ошибка выставления заявки на продажу.", figi);
                    return Uni.createFrom().nothing();
                });
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
        return toUni(ordersService.cancelOrder(this.accountId, orderId))
                .invoke(() -> eventLogger.log("Отменена заявка orderId=" + orderId))
                .onFailure()
                .recoverWithUni(throwable -> {
                    exceptionHandler.handle(throwable, "Ошибка отмены заявки orderId=" + orderId);
                    return Uni.createFrom().nothing();
                });
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
