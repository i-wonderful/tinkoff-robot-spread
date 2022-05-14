package com.byby.trobot.executor.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.dto.mapper.PortfolioMapper;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.service.impl.SpreadService;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.byby.trobot.dto.mapper.PortfolioMapper.*;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.bigDecimalToQuotation;

/**
 * Операции с песочницей
 */
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
@ApplicationScoped
public class SandboxExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);
    private static final int QUANTITY_DEFAULT = 1;

    private SharesService sharesService;
    private SandboxService sandboxService;
    private SpreadService spreadService;
    private PortfolioMapper portfolioMapper;
    private EventBus bus;
    private EventLogger eventLogger;

    private String accountId;

    public SandboxExecutor(InvestApi api, SharesService sharesService, EventBus bus, EventLogger eventLogger, SpreadService spreadService, PortfolioMapper portfolioMapper) {
        log.info(">>> Init sandboxExecutor");
        this.sharesService = sharesService;
        this.sandboxService = api.getSandboxService();
        this.bus = bus;
        this.eventLogger = eventLogger;
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
                    .orElseGet(this::createNewAccount)
                    .getId();
        }
        return accountId;
    }

    /**
     * Выставить лимитную заявку на покупку.
     */
    @Override
    public PostOrderResponse postBuyLimitOrder(String figi, BigDecimal price) {
        PostOrderResponse response = sandboxService.postOrderSync(figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_BUY,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString());

        //eventLogger.log(String.format("Выставлена лимитная заявка на покупку по цене %f, orderId=%s", price.doubleValue(), response.getOrderId()), figi);

        return response;
    }

    @Override
    public PostOrderResponse postSellLimitOrder(String figi, BigDecimal price) {
        PostOrderResponse response = sandboxService.postOrderSync(figi,
                QUANTITY_DEFAULT,
                bigDecimalToQuotation(price),
                OrderDirection.ORDER_DIRECTION_SELL,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString());

        //eventLogger.log(String.format("Выставлена лимитная заявка на покупку по цене %f, orderId=%s", price.doubleValue(), response.getOrderId()), figi);

        return response;
    }

    /**
     * Проверяем будет ли наша виртуальная заявка myBuyOrder оптимальной.
     * В сандбоксе заявка не существует в реальном стакане,
     * поэтому ставнивем цену моей заявки и цену на шаг выше заявки на покупку из стакана.
     *
     * @param myBuyOrder заявка песочницы
     * @param bidFromOrderbook верхняя заявка на покупку из стакана
     * @return является ли мой заявка оптимальной
     */
    @Override
    public boolean isMyBuyOrderOptimal(OrderState myBuyOrder, Order bidFromOrderbook) {
        if (bidFromOrderbook == null) {
            log.warn(">>> Bid form orderbook is null.");
            return true;
        }
        BigDecimal nextBidPrice = quotationToBigDecimal(spreadService.calcNextBidPrice(
                myBuyOrder.getFigi(),
                bidFromOrderbook.getPrice()));
        BigDecimal myBidPrice = moneyValueToBigDecimal(myBuyOrder.getInitialSecurityPrice());

        return  nextBidPrice.compareTo(myBidPrice) == 0;
    }

    /**
     * Проверяем будет ли наша виртуальная заявка myOrderSell оптимальной.
     * В сандбоксе заявка не существует в реальном стакане,
     * поэтому ставнивем цену моей заявки и цену на шаг ниже заявки на продажу из стакана.
     *
     * @param myOrderSell заявка песочницы
     * @param askFromOrderbook
     * @return
     */
    @Override
    public boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook) {
        if (askFromOrderbook == null) {
            log.warn(">>> Ask form orderbook is null.");
            return true;
        }
        BigDecimal nextAskPrice = quotationToBigDecimal(spreadService.calcNextAskPrice(
                myOrderSell.getFigi(),
                askFromOrderbook.getPrice()));
        BigDecimal myAskPrice = moneyValueToBigDecimal(myOrderSell.getInitialSecurityPrice());

        return nextAskPrice.compareTo(myAskPrice) == 0;
    }

    @Override
    public void cancelOrder(String orderId) {
        log.info(">>> cancel Order Sandbox 1, orderId= " + orderId);
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


//    public Multi<OrderState> getMyOrders1() {
//        return Multi.createFrom()
//                .completionStage(sandboxService.getOrders(getAccountId()));

//        return Uni.createFrom()
//                .completionStage(sandboxService.getOrders(getAccountId()));
//    }


    private Account createNewAccount() {
        // todo
        log.info(">>> Create new account");
        return null;
    }

}
