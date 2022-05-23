package com.byby.trobot.service.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.controller.exception.BusinessException;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.executor.Executor;
import com.byby.trobot.service.ExchangeService;
import com.byby.trobot.service.StatisticService;
import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.FindFigiService;
import com.byby.trobot.strategy.Strategy;
import com.byby.trobot.cache.StrategyCacheManager;
import com.google.common.collect.Streams;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    StrategySharesProperties strategySharesProperties;
    
    @Inject
    SharesServiceImpl sharesService;

    @Inject
    ExchangeService exchangeService;

    @Inject
    StatisticService statisticService;

    // запущена ли в данный момент
    private boolean isRun = false;
    // найдено ли нужное количество акций
    private boolean isAllFigiFind = false;

    /**
     * Запустить стратегию.
     *
     * @return
     */
    @Override
    public Uni<Void> start() {
        if(exchangeService.getExchangesOpenNow().isEmpty()) {
            throw new BusinessException("Нет открытых бирж в данный момент.");
        }
        if (this.isRun) {
            eventLogger.log("Уже запущен");
            return Uni.createFrom().voidItem();
        }

        eventLogger.log("Поехали!");
        return startFindFigi()
                .onItem()
                .invoke(() -> {
                    this.isRun = true;
                    this.isAllFigiFind = false;
                })
                .call(() -> statisticService.start().replaceWithVoid());
    }


    /**
     *
     * @return
     */
    @Override
    public Uni<Void> stop() {
        this.isRun = false;
        this.isAllFigiFind = false;

        return cacheManager.getFigi()
                .call(figi -> strategy.stopListening(figi))
                .call(() -> cacheManager.clear())
                .call(() -> statisticService.stop())
                .invoke(() -> {
                    findFigiService.stopTimers();
                    eventLogger.log("Остановлен.");
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
     * @return
     */
    public Uni<Void> startFindFigi() {
        return figiFromProperties()
                .call(figi -> {
                    if (figi == null || figi.isEmpty()) {
                        return findFigiService.startFindFigi();
                    } else {
                        bus.send(GlobalBusAddress.NEW_FIGI, figi.stream().collect(Collectors.joining(",")));
                        eventLogger.log("Отслеживаем акции из настроек", figi);
                        return Uni.createFrom().voidItem();
                    }
                })
                .replaceWithVoid();
    }

    /**
     * Загрузить из настроек акции
     * на которых будет работать стратегия.
     *
     * @return список figi
     */
    private Uni<List<String>> figiFromProperties() {
        List<String> tickers = strategySharesProperties.tickersFind().orElse(Collections.emptyList());
        if (tickers.isEmpty()) {
            return Uni.createFrom().item(Collections.emptyList());
        }
        eventLogger.log(String.format("Будем торговать акциями из настроек tickers=%s", tickers));
        return sharesService.findFigiByTicker(tickers);
    }

    /**
     * Обработчик события: Найдены новые подходящие акции.
     * <p>
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
            eventLogger.log("Пока не нашли продходящие акции. Ждем поиск следующих по таймеру через минуту.");
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
                    int maxCount = strategySharesProperties.maxCount();
                    List<String> allAddToCache = Streams.concat(oldFigi.stream(), newFigi.stream())
                            .limit(maxCount)
                            .collect(Collectors.toList());
                    if (allAddToCache.size() == maxCount) {
                        eventLogger.log("Найдено нужное количество акций " + maxCount);
                        this.isAllFigiFind = true;
                        findFigiService.stopTimers();
                    }
                    return allAddToCache;
                })
                .call(allAddToCache -> cacheManager.clearAndAddFigi(allAddToCache))
                .onFailure()
                .transform((throwable) -> new CriticalException(throwable, "Ошибка запуска стратегии"))
                .subscribe()
                .with(strategy::start);
    }

}
