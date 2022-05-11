package com.byby.trobot.service.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.Strategy;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.byby.trobot.common.GlobalBusAddress.LOG;

@ApplicationScoped
public class StrategyManagerImpl implements StrategyManager {
    private static final Logger log = LoggerFactory.getLogger(StrategyManagerImpl.class);

    @Inject
    EventLogger eventLogger;

    @Inject
    Strategy strategy;

    @Inject
    EventBus bus;

    @Inject
    Instance<Executor> executor;

    // todo как этим пользоваться
    @Inject
    ThreadContext threadContext;
    @Inject
    ManagedExecutor managedExecutor;

    // todo сделать нормальный кеш
    Map<String, List<String>> cacheOrders = new HashMap<>();

    @Override
    public void go() {
        eventLogger.log("Поехали!");

        List<String> figi = strategy.findFigi();
        if (figi == null || figi.isEmpty()) {
            eventLogger.log("Не найдены подходящие акции. Измените настройки.");
            return;
        } else {
            eventLogger.log("Отслеживаем акции", figi);
        }

        strategy.go(figi);
    }

    @ConsumeEvent(GlobalBusAddress.portBuyOrder)
    public void postBuyOrder(String figi) {

        PostOrderResponse response = executor.get().postBuyOrder(figi);
        String orderId = response.getOrderId();

        cacheOrders.put(figi, List.of(orderId));
    }

    @ConsumeEvent(GlobalBusAddress.cancelBuyOrder)
    public void cancelBuyOrder(String figi) {
        eventLogger.log(">>> Cancel Buy Order", figi);

        List<String> orders = cacheOrders.get(figi);
        orders.forEach(orderId -> {
                    log.info(">>> OrderId " + orderId);
                    executor.get().cancelBuyOrder(orderId);
                }
        );

    }

    @Override
    public void sellAll() {

        // todo
    }

}
