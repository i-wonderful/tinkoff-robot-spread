package com.byby.trobot.executor.impl;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.RobotSandboxProperties;
import com.byby.trobot.controller.dto.PortfolioDto;
import com.byby.trobot.controller.dto.mapper.PortfolioMapper;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.SandboxAccountService;
import com.byby.trobot.service.SpreadService;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.byby.trobot.executor.impl.Helper.findOpenAccountId;
import static com.byby.trobot.executor.impl.Helper.toUni;
import static ru.tinkoff.piapi.core.utils.MapperUtils.*;

/**
 * Операции с песочницей
 */
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
@ApplicationScoped
public class SandboxExecutor implements Executor, SandboxAccountService {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);
    private static final int QUANTITY_DEFAULT = 1;

    private SandboxService sandboxService;
    private SpreadService spreadService;
    private PortfolioMapper portfolioMapper;
    private RobotSandboxProperties properties;
    private AppCache appCache;
    private EventLogger eventLogger;
    private String accountId;

    public SandboxExecutor(RobotSandboxProperties properties, InvestApi api, SpreadService spreadService, PortfolioMapper portfolioMapper, AppCache appCache, EventLogger eventLogger) {
        log.info(">>> Init SandboxExecutor");
        this.properties = properties;
        this.sandboxService = api.getSandboxService();
        this.spreadService = spreadService;
        this.portfolioMapper = portfolioMapper;
        this.appCache = appCache;
        this.eventLogger = eventLogger;
    }

    @Override
    public Uni<String> loadAccountId() {
        if (accountId != null) {
            return Uni.createFrom().item(accountId);
        }
        log.info(">>> loadAccountId");
        return toUni(sandboxService.getAccounts())
                .onFailure()
                .transform(throwable -> new CriticalException(throwable, "Не удалось получить аккаунт песочницы! Проверьте токен и доступ."))
                .onItem()
                .transformToUni(accounts -> {
                            Optional<String> accountIdOpt = findOpenAccountId(accounts);
                            accountIdOpt.ifPresent(accountId -> {
                                appCache.putAccountId(accountId);
                                this.accountId = accountId;
                            });
                            return accountIdOpt
                                    .map(accountId -> Uni.createFrom().item(accountId))
                                    .orElseGet(this::createNewAccount);
                        }
                );
    }


    /**
     * Выставить лимитную заявку на покупку.
     */
    @Override
    public Uni<PostOrderResponse> postBuyLimitOrder(String figi, BigDecimal price) {
        return toUni(sandboxService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_BUY,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    /**
     * Выставить лимитную заявку на продажу.
     */
    @Override
    public Uni<PostOrderResponse> postSellLimitOrder(String figi, BigDecimal price) {
        return toUni(sandboxService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_SELL,
                this.accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    /**
     * Проверяем будет ли наша виртуальная заявка песочницы myBuyOrder оптимальной.
     * В сандбоксе заявка не существует в реальном стакане,
     * поэтому сравниваем цену моей виртуальной заявки и цену на шаг выше заявки на покупку из стакана.
     * Если они одинаковы, то заявка оптимальна.
     *
     * @param myBuyOrder       моя заявка песочницы
     * @param bidFromOrderbook верхняя заявка на покупку из стакана
     * @return является ли моя заявка оптимальной
     */
    @Override
    public boolean isMyBuyOrderOptimal(OrderState myBuyOrder, Order bidFromOrderbook) {
        if (bidFromOrderbook == null) {
            log.warn(">>> Bid form orderbook is null.");
            return true;
        }
        BigDecimal nextBidPrice = quotationToBigDecimal(
                spreadService.calcNextBidPrice(
                                myBuyOrder.getFigi(),
                                bidFromOrderbook.getPrice())
                        .await().indefinitely()); // todo ?
        BigDecimal myBidPrice = moneyValueToBigDecimal(myBuyOrder.getInitialSecurityPrice());

        return nextBidPrice.compareTo(myBidPrice) == 0;
    }

    /**
     * Проверяем будет ли наша виртуальная заявка myOrderSell оптимальной.
     * В сандбоксе заявка не существует в реальном стакане,
     * поэтому ставнивем цену моей заявки и цену на шаг ниже заявки на продажу из стакана.
     *
     * @param myOrderSell      заявка песочницы
     * @param askFromOrderbook
     * @return
     */
    @Override
    public boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook) {
        if (askFromOrderbook == null) {
            log.warn(">>> Ask form orderbook is null.");
            return true;
        }
        BigDecimal nextAskPrice = quotationToBigDecimal(
                spreadService.calcNextAskPrice(myOrderSell.getFigi(), askFromOrderbook.getPrice())
                        .await().indefinitely()); //  todo ?
        BigDecimal myAskPrice = moneyValueToBigDecimal(myOrderSell.getInitialSecurityPrice());

        return nextAskPrice.compareTo(myAskPrice) == 0;
    }


    /**
     * Отменить заявку.
     */
    @Override
    public Uni<Instant> cancelOrder(String orderId) {
        return Uni.createFrom()
                .completionStage(sandboxService.cancelOrder(this.accountId, orderId))
                .invoke(() -> eventLogger.log("Отменена заявка orderId=" + orderId));
    }

    @Override
    public Uni<PortfolioDto> getPortfolio() {
        return loadAccountId()
                .onItem()
                .transformToUni(accountId ->
                        getPortfolio(accountId)
                                .onItem()
                                .transform(portfolioResponse -> portfolioMapper.toDto(portfolioResponse, accountId)));
    }

    private Uni<PortfolioResponse> getPortfolio(String accountId) {
        return Uni.createFrom()
                .completionStage(sandboxService.getPortfolio(accountId));
    }

    @Override
    public Uni<List<OrderState>> getMyOrders() {
        log.info(">>> API Call: sandboxService.getOrders(...)");
        return loadAccountId()
                .onItem()
                .transformToUni(accountId -> Uni.createFrom()
                        .completionStage(sandboxService.getOrders(accountId)));
    }

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
    public Uni<String> recreateSandbox() {
        return Uni.createFrom()
                .completionStage(sandboxService.closeAccount(this.accountId))
                .onItem()
                .transformToUni(v -> createNewAccount());
    }

    private Uni<String> createNewAccount() {
        log.info(">>> API Call: sandboxService.openAccountSync");
        return toUni(sandboxService.openAccount())
                .onItem()
                .call(accountId -> {
                    MoneyValue balanceRub = bigDecimalToMoneyValue(properties.initBalanceRub());
                    MoneyValue balanceUsd = bigDecimalToMoneyValue(properties.initBalanceUsd(), "USD");
                    CompletableFuture payRub = sandboxService.payIn(accountId, balanceRub);
                    CompletableFuture payUsd = sandboxService.payIn(accountId, balanceUsd);
                    CompletableFuture.allOf(payRub, payUsd).join();
                    appCache.putAccountId(accountId);
                    this.accountId = accountId;
                    return Uni.createFrom().item(accountId);
                })
                .onFailure()
                .transform(throwable -> new CriticalException(throwable, "Не удалось создать новый аккаунт песочницы!"));
    }

}
