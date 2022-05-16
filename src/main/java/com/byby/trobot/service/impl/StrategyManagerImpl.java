package com.byby.trobot.service.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.FindFigiService;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.strategy.impl.StrategyCacheManager;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class StrategyManagerImpl implements StrategyManager {
    private static final Logger log = LoggerFactory.getLogger(StrategyManagerImpl.class);

    @Inject
    EventLogger eventLogger;

    @Inject
    Strategy strategy;
    @Inject
    FindFigiService findFigiService;
    @Inject
    EventBus bus;

    @Inject
    Instance<Executor> executor;

    // todo как этим пользоваться
//    @Inject
//    ThreadContext threadContext;
//    @Inject
//    ManagedExecutor managedExecutor;

    @Inject
    StrategyCacheManager cacheManager;

    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    private boolean isRun = false;

    @Override
    public void start() {
        if (this.isRun) {
            eventLogger.log("Уже запущен");
            return;
        }
        eventLogger.log("Поехали!");
        findFigi();
        this.isRun = true;
    }

    /**
     * Найдены новые подходящие акции.
     * Запускаем стратегию.
     */
    @ConsumeEvent(value = GlobalBusAddress.NEW_FIGI, blocking = true)
    public void newFigiFoundAndStartStrategy(String figiArray) {
        List<String> figi = Arrays.asList(figiArray.split(","));
        log.info(">>> New Figi found: " + figi);

        cacheManager.getFigi()
                .invoke((oldFigi) -> strategy.stopListening(oldFigi))
                .onItem()
                //.call((oldFigi) -> strategy.stopListening(oldFigi))
                .transformToUni((t) -> cacheManager.addFigi(figi))
                .subscribe()
                .with(figiAll -> {
                    strategy.start(figiAll);
                });
    }

    @Override
    public void stop() {
        if (!this.isRun) {
            eventLogger.log("Не запущен");
            return;
        }
        strategy.stop().subscribeAsCompletionStage().join();
        this.isRun = false;
    }

    @Override
    public void cancelAllOrders() {
        // todo
        executor.get().cancelAllOrders();
    }

    @Override
    public boolean isRun() {
        return this.isRun;
    }

    /**
     * Ищем кандидатов на покупку.
     * Если указан параметр robot.strategy.find.buy.tickers, то берем их.
     * Если не указан, то ищем согласно стратегии.
     *
     * @return список figi
     */
    public Uni<List<String>> findFigi() {
        List<String> figi = figiFromProperties();
        if (figi.isEmpty()) {
            return findFigiService.findFigi();
        }
        if (figi == null || figi.isEmpty()) {
            eventLogger.log("Не найдены подходящие акции. Измените настройки.");
        } else {
            eventLogger.log("Отслеживаем акции", figi);
        }

        return Uni.createFrom().item(figi);
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



//    @ConsumeEvent(value = GlobalBusAddress.POST_BUY_ORDER, blocking = true)
//    // todo сделать полностью неблокирующий вызов
//    public void postBuyLimitOrder(String figi) {
//
//        //PostOrderResponse response = executor.get().postBuyLimitOrder(figi, );
//        //String orderId = response.getOrderId();
//
//       // cacheOrders.put(figi, List.of(orderId));
//    }


//    @ConsumeEvent(value = GlobalBusAddress.CANCEL_ORDER, blocking = true)
//    public void cancelOrder(String orderId) {
//        log.info(">>> Cancel order " + orderId);
//
//        executor.get().cancelOrder(orderId);
////        eventLogger.log(">>> Cancel Buy Order", figi);
////
////        List<String> orders = cacheOrders.get(figi);
////        orders.forEach(orderId -> {
////                    log.info(">>> OrderId " + orderId);
////                    executor.get().cancelBuyOrder(orderId);
////                }
////        );
//
//    }


}
