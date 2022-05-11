package com.byby.trobot.executor.impl;

import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.SharesService;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

import static com.byby.trobot.dto.mapper.PortfolioMapper.*;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

/**
 * Операции с песочницей
 */
//@Startup
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
@ApplicationScoped
public class SandboxExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);

    private SharesService sharesService;
    private SandboxService sandboxService;
    private EventBus bus;

    private String accountId;

    public SandboxExecutor(InvestApi api, SharesService sharesService, EventBus bus) {
        log.info(">>> Init sandboxExecutor");
        this.sharesService = sharesService;
        this.sandboxService = api.getSandboxService();
        this.bus = bus;
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
    @Override
    public PostOrderResponse postBuyOrder(String figi) {

        Quotation price = sharesService.calcMinBuyPrice(figi);

        PostOrderResponse response = sandboxService.postOrderSync(figi,
                1,
                price,
                OrderDirection.ORDER_DIRECTION_BUY,
                getAccountId(),
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString());

        String logMessage = String.format("[figi=%s] Выставлена заявка на покупку по цене %f, orderId=%s", figi, quotationToBigDecimal(price).doubleValue(), response.getOrderId());
        bus.publish(GlobalBusAddress.LOG.name(), logMessage);
        log.info(logMessage);

        return response;
    }

    @Override
    public void cancelBuyOrder(String orderId) {
        sandboxService.cancelOrderSync(getAccountId(), orderId);
        log.info(">>> cancelBuyOrder Sandbox");
    }

    @Override
    public PortfolioDto getPortfolio() {
        var portfolio = sandboxService.getPortfolioSync(getAccountId());
        return toDto(portfolio, getAccountId());
    }


    private Account createNewAccount() {
        // todo
        log.info(">>> Create new account");
        return null;
    }

}
