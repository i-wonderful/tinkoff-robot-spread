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

    @Inject
    StrategyCacheManager cacheManager;

    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    // запущена ли в данный момент
    private boolean isRun = false;
    // найдено ли нужное количество акций
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
        }

        return cacheManager.getFigi()
                .call(figi -> strategy.stopListening(figi))
                .call(() -> cacheManager.clear())
                .call(() -> eventLogger.log("Остановлен."))
                .invoke(() -> {
                    this.isRun = false;
                    this.isAllFigiFind = false;
                })
                .replaceWithVoid();
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
     * Если указан параметр в настройках robot.strategy.find.buy.tickers, то берем их.
     * Если не указан, то ищем согласно условиям отбора стратегии.
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

    /**
     * Загрузить из настроек акции
     * на которых будет работать стратегия.
     *
     * @return список figi
     */
    private List<String> figiFromProperties() {
        List<String> tickers = properties.getFindBuyTickers();
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }
        eventLogger.log(String.format("Будем торговать акциями из настроек tickers=%s", tickers));
        return sharesService.findByTicker(tickers).stream()
                .map(Share::getFigi)
                .collect(Collectors.toList());
    }

    /**
     * Обработчик события: Найдены новые подходящие акции.
     *
     * Оперирует с новым найденными и уже имеющимися акциями.
     * Сохраняет в кеше.
     * Запускаем стратегию.
     *
     * @param newFigiArray список новых подходящих акций (figi через запятую)
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

        // Берем акции из кэша по которым уже запущена стратегия, останавливаем подписку на стаканы.
        // Новые найденные акции добавляем к списку из кэша, оставляем нужное их количество указанное в настройках,
        // если нужное количесво акций найдено, останавливаем таймеры.
        // Кладем все нужные акции в кеш.
        // Запускаем стратегию.
        cacheManager.getFigi()
                .call((oldFigi) -> strategy.stopListening(oldFigi))
                .onItem()
                .transform(oldFigi -> {
                    List<String> allAddToCache = Streams.concat(oldFigi.stream(), newFigi.stream())
                            .limit(properties.getSharesMaxCount())
                            .collect(Collectors.toList());
                    if (allAddToCache.size() == properties.getSharesMaxCount()) {
                        eventLogger.log("Найдено нужное количество акций " + properties.getSharesMaxCount());
                        this.isAllFigiFind = true;
                        findFigiService.stopTimers();
                    }
                    return allAddToCache;
                })
                .call(allAddToCache -> cacheManager.clearAndAddFigi(allAddToCache))
                .subscribe()
                .with(strategy::start);
    }

}
