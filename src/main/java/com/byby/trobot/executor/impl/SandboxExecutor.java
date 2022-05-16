package com.byby.trobot.executor.impl;

import com.byby.trobot.config.SandboxProperties;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.mapper.PortfolioMapper;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.SandboxAccountService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.service.impl.SpreadService;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ru.tinkoff.piapi.core.utils.MapperUtils.*;

/**
 * Операции с песочницей
 */
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
@ApplicationScoped
public class SandboxExecutor implements Executor, SandboxAccountService {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);
    private static final int QUANTITY_DEFAULT = 1;

    private SharesService sharesService;
    private SandboxService sandboxService;
    private SpreadService spreadService;
    private PortfolioMapper portfolioMapper;
    private SandboxProperties properties;

    private String accountId;

    public SandboxExecutor(SandboxProperties properties, InvestApi api, SharesService sharesService, SpreadService spreadService, PortfolioMapper portfolioMapper) {
        log.info(">>> Init sandboxExecutor ");
        this.properties = properties;
        this.sharesService = sharesService;
        this.sandboxService = api.getSandboxService();
        this.spreadService = spreadService;
        this.portfolioMapper = portfolioMapper;
    }

    @Override
    public String getAccountId() {
        if (accountId == null) {
            accountId = sandboxService.getAccountsSync()
                    .stream()
                    .filter(account -> AccountStatus.ACCOUNT_STATUS_OPEN.equals(account.getStatus()))
                    .findFirst()
                    .map(Account::getId)
                    .orElseGet(this::createNewAccount);
        }
        return accountId;
    }

    /**
     * Выставить лимитную заявку на покупку.
     */
    @Override
    public Uni<PostOrderResponse> postBuyLimitOrder(String figi, BigDecimal price) {
        return Uni.createFrom().completionStage(sandboxService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_BUY,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    @Override
    public Uni<PostOrderResponse> postSellLimitOrder(String figi, BigDecimal price) {
        return Uni.createFrom().completionStage(sandboxService.postOrder(
                figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_SELL,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString()));
    }

    /**
     * Проверяем будет ли наша виртуальная заявка myBuyOrder оптимальной.
     * В сандбоксе заявка не существует в реальном стакане,
     * поэтому ставнивем цену моей заявки и цену на шаг выше заявки на покупку из стакана.
     *
     * @param myBuyOrder       заявка песочницы
     * @param bidFromOrderbook верхняя заявка на покупку из стакана
     * @return является ли мой заявка оптимальной
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

    @Override
    public void cancelOrder(String orderId) {
        log.info(">>> cancel Order Sandbox 1, orderId=" + orderId);
        sandboxService.cancelOrder(getAccountId(), orderId);
        log.info(">>> cancel Order Sandbox 2, orderId= " + orderId);
    }

    @Override
    public Uni<PortfolioDto> getPortfolio() {
        return Uni.createFrom()
                .completionStage(sandboxService.getPortfolio(getAccountId()))
                .onItem()
                .transform(portfolioResponse -> portfolioMapper.toDto(portfolioResponse, getAccountId()));
    }

    @Override
    public Uni<List<OrderState>> getMyOrders() {
        return Uni.createFrom()
                .completionStage(sandboxService.getOrders(getAccountId()));
    }

    @Override
    public Uni cancelAllOrders() {
        getMyOrders()
                .subscribe()
                .with(orderStates -> orderStates.forEach(o -> cancelOrder(o.getOrderId())));
        return Uni.createFrom().voidItem();
    }

    @Override
    public void recreateSandbox() {
        sandboxService.closeAccount(getAccountId());
        this.accountId = createNewAccount();
    }

    private String createNewAccount() {
        log.info(">>> Create new sandbox account");
        String accountId = sandboxService.openAccountSync();
        MoneyValue balanceRub = bigDecimalToMoneyValue(properties.getInitBalanceRub());
        MoneyValue balanceUsd = bigDecimalToMoneyValue(properties.getInitBalanceUsd(), "USD");
        CompletableFuture payRub = sandboxService.payIn(accountId, balanceRub);
        CompletableFuture payUsd = sandboxService.payIn(accountId, balanceUsd);
        CompletableFuture.allOf(payRub, payUsd).join();
        return accountId;
    }

}
