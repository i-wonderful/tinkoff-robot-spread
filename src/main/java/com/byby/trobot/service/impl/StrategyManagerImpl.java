package com.byby.trobot.service.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.ApplicationProperties;
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
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    @Override
    public void start() {
        eventLogger.log("Поехали!");

        List<String> figi = figiFromProperties();
        if (figi.isEmpty()) {
            figi = strategy.findFigi();
        }
        if (figi == null || figi.isEmpty()) {
            eventLogger.log("Не найдены подходящие акции. Измените настройки.");
            return;
        } else {
            eventLogger.log("Отслеживаем акции", figi);
        }

        strategy.start(figi);
    }

    private List<String> figiFromProperties() {
        List<String> tickers = properties.getFindBuyTickers();

        if (tickers != null && !tickers.isEmpty()) {
            eventLogger.log(String.format("Будем торговать акциями из настроек tickers=%s", tickers));
            return sharesService.findByTicker(tickers).stream()
                    .map(Share::getFigi)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @ConsumeEvent(value = GlobalBusAddress.POST_BUY_ORDER, blocking = true)
    // todo сделать полностью неблокирующий вызов
    public void postBuyLimitOrder(String figi) {

        PostOrderResponse response = executor.get().postBuyLimitOrder(figi);
        String orderId = response.getOrderId();

        cacheOrders.put(figi, List.of(orderId));
    }


    @ConsumeEvent(value = GlobalBusAddress.CANCEL_ORDER, blocking = true)
    public void cancelOrder(String orderId) {
        log.info(">>> Cancel order " + orderId);

        executor.get().cancelOrder(orderId);
//        eventLogger.log(">>> Cancel Buy Order", figi);
//
//        List<String> orders = cacheOrders.get(figi);
//        orders.forEach(orderId -> {
//                    log.info(">>> OrderId " + orderId);
//                    executor.get().cancelBuyOrder(orderId);
//                }
//        );

    }

    @Override
    public void sellAll() {

        // todo
    }

}
