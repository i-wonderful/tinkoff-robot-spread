package com.byby.trobot.executor.impl;

import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.impl.SharesService;
import io.quarkus.vertx.ConsumeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import java.util.UUID;

import static com.byby.trobot.dto.mapper.PortfolioMapper.*;

/**
 * Операции с песочницей
 */
//@ApplicationScoped
//@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "true")
public class SandboxExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(SandboxExecutor.class);

    private SharesService sharesService;
    private SandboxService sandboxService;

    private String accountId;

    public SandboxExecutor(InvestApi api, SharesService sharesService) {
        this.sharesService = sharesService;
        this.sandboxService = api.getSandboxService();
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
     * Продать акцию
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
                accountId,
                OrderType.ORDER_TYPE_LIMIT,
                UUID.randomUUID().toString());

        log.info(">>> Response: " + response);

        return response;
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
