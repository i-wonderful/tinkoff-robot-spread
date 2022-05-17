package com.byby.trobot.service.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.FindFigiService;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.cache.StrategyCacheManager;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
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
    private boolean isAllFigiFind = false;

    @Override
    public Uni<Void> start() {
        if (this.isRun) {
            eventLogger.log("Уже запущен");
            return Uni.createFrom().voidItem();
        }
        eventLogger.log("Поехали!");
        return runFindFigi()
                .invoke(() -> {
                    this.isRun = true;
                    this.isAllFigiFind = false;
                })
                .replaceWithVoid();
    }


    @Override
    public Uni<Void> stop() {
        if (!this.isRun) {
            eventLogger.log("Не запущен");
            return Uni.createFrom().voidItem();
        } else {
            this.isRun = false;
            this.isAllFigiFind = false;
        }

        return Uni.combine().all().unis(cacheManager.clear(), strategy.stop()).discardItems();
    }

    @Override
    public Uni<Void> cancelAllOrders() {
        return executor.get().cancelAllOrders();
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
    public Uni<List<String>> runFindFigi() {
        List<String> figi = figiFromProperties();
        if (figi == null || figi.isEmpty()) {
            return findFigiService.findFigi();
        } else {
            eventLogger.log("Отслеживаем акции из настроек", figi);
            bus.send(GlobalBusAddress.NEW_FIGI, figi.stream().collect(Collectors.joining(",")));
            return Uni.createFrom().item(figi);
        }
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

    /**
     * Event: Найдены новые подходящие акции.
     * Запускаем стратегию.
     */
    @ConsumeEvent(value = GlobalBusAddress.NEW_FIGI, blocking = true)
    public void newFigiFoundAndStartStrategy(String newFigiArray) {
        List<String> newFigi = Arrays.asList(newFigiArray.split(","));
        log.info(">>> New Figi found: " + newFigi);

        if (this.isAllFigiFind) {
            log.info(">>> Already all find");
            return;
        }

        if (newFigi.isEmpty()) {
            eventLogger.log("Пока не нашли продходящие акции. Ждем поиск следующих по таймеру.");
            return;
        }

        cacheManager.getFigi()
                .invoke((oldFigi) -> strategy.stopListening(oldFigi))
                .onItem()
                .transformToUni(oldFigi -> {

                    List<String> addToCache = Streams.concat(oldFigi.stream(), newFigi.stream()).collect(Collectors.toList());
                    if (addToCache.size() >= properties.getSharesMaxCount()) {
                        // todo сделать остановку тайметор
                        eventLogger.log("Найдено нужное количество акций " + properties.getSharesMaxCount());
                        addToCache = addToCache.subList(0, properties.getSharesMaxCount());
                        this.isAllFigiFind = true;
                        findFigiService.stopTimers();
                    }
                    log.info(">>> Добавляем в кеш: " + addToCache);
                    return cacheManager
                            .addFigi(addToCache)
                            .invoke(allFigi -> eventLogger.log("Отслеживаем акции", allFigi));
                })
                .subscribe()
                .with(strategy::start);
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
