package com.byby.trobot.executor.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.SharesService;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

import static com.byby.trobot.dto.mapper.PortfolioMapper.*;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

/**
 * Операции с песочницей
 */
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
@ApplicationScoped
public class SandboxExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);

    private SharesService sharesService;
    private SandboxService sandboxService;
    private EventBus bus;
    private EventLogger eventLogger;

    private String accountId;

    public SandboxExecutor(InvestApi api, SharesService sharesService, EventBus bus, EventLogger eventLogger) {
        log.info(">>> Init sandboxExecutor");
        this.sharesService = sharesService;
        this.sandboxService = api.getSandboxService();
        this.bus = bus;
        this.eventLogger = eventLogger;
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
     * Купить акцию
     *
     * @param figi
     * @return
     */
    // todo цена
    @Override
    public PostOrderResponse postBuyLimitOrder(String figi) {
        Quotation price = sharesService.calcMinBuyPrice(figi);

        PostOrderResponse response = sandboxService.postOrderSync(figi,
                1,
                price,
                OrderDirection.ORDER_DIRECTION_BUY,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString());

        eventLogger.log(String.format("Выставлена лимитная заявка на покупку по цене %f, orderId=%s", quotationToBigDecimal(price).doubleValue(), response.getOrderId()), figi);

        return response;
    }

    @Override
    public PostOrderResponse postSellLimitOrder(String figi) {
        log.info(">>> todo Post Sell Limit Order");
        return null;
    }

    @Override
    public void cancelOrder(String orderId) {
        log.info(">>> cancel Order Sandbox 1, orderId= " + orderId);
        sandboxService.cancelOrderSync(getAccountId(), orderId);
        log.info(">>> cancel Order Sandbox 2, orderId= " + orderId);
    }

    @Override
    public PortfolioDto getPortfolio() {
        var portfolio = sandboxService.getPortfolioSync(getAccountId());
        return toDto(portfolio, getAccountId());
    }

    @Override
    public Uni<List<OrderState>> getOrders() {
        return Uni.createFrom()
                .completionStage(sandboxService.getOrders(getAccountId()));
    }


    private Account createNewAccount() {
        // todo
        log.info(">>> Create new account");
        return null;
    }

}
