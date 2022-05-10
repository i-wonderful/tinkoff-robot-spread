package com.byby.trobot.service.impl;

import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.Strategy;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;

import static com.byby.trobot.config.GlobalBusAddress.LOG;

@ApplicationScoped
public class StrategyManagerImpl implements StrategyManager {
    private static final Logger log = LoggerFactory.getLogger(StrategyManagerImpl.class);

    @Inject
    Strategy strategy;

    @Inject
    EventBus bus;

    @Inject
    Instance<Executor> executor;

    @Override
    public void go() {
        bus.publish(LOG.name(), "Поехали!");

        List<String> figi = strategy.findFigi();
        bus.publish(LOG.name(), "Отслеживаем акции: " + figi);

        strategy.go(figi);
    }

    @ConsumeEvent("postBuyOrder")
    public void postBuyOrder(String figi){
        executor.get().postBuyOrder(figi);
    }

    @Override
    public void sellAll() {

        // todo
    }
}
