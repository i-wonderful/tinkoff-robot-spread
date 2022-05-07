package com.byby.trobot.executor.impl;

import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.core.InvestApi;

//@ApplicationScoped
//@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "false")
public class RealExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(RealExecutor.class);

    private InvestApi api;

    public RealExecutor(InvestApi api) {
        this.api = api;
    }

    @Override
    public String getAccountId() {
        // todo
        return null;
    }

    @Override
    public PostOrderResponse postBuyOrder(String figi) {
        // todo
        return null;
    }

    @Override
    public PortfolioDto getPortfolio() {
        // todo
        return null;
    }
}
