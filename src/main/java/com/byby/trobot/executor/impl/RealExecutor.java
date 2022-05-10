package com.byby.trobot.executor.impl;

import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;

//@Startup
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "false")
@ApplicationScoped
public class RealExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(RealExecutor.class);

    private InvestApi api;

    public RealExecutor(InvestApi api) {
        log.info(">>> Init Real Executor");
        this.api = api;
    }

    @Override
    public String getAccountId() {
        // todo
        return null;
    }

    @Override
    public PostOrderResponse postBuyOrder(String figi) {
        log.info(">>> Real. Выставить заявку на покупку");
        // todo
        return null;
    }

    @Override
    public void cancelBuyOrder(String figi) {
        log.info(">>> cancelBuyOrder Real ");
    }

    @Override
    public PortfolioDto getPortfolio() {
        // todo
        return null;
    }
}
