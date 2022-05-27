package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.controller.handler.ExceptionHandler;
import com.byby.trobot.service.ExchangeService;
import com.byby.trobot.service.impl.SharesServiceImpl;
import com.byby.trobot.service.SpreadService;
import com.byby.trobot.strategy.FindFigiService;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.byby.trobot.common.GlobalBusAddress.NEW_FIGI;

/**
 * Сервис поиска акций среди всех акций.
 */
@ApplicationScoped
public class SpreadFindFigiService implements FindFigiService {
    private static final Logger log = LoggerFactory.getLogger(SpreadStrategy.class);

    @Inject
    EventLogger eventLogger;

    @Inject
    Vertx vertx;
    @Inject
    EventBus bus;
    @Inject
    ExchangeService exchangeService;
    @Inject
    SharesServiceImpl sharesService;
    @Inject
    SpreadService spreadService;
    @Inject
    SpreadDecision spreadDecision;

    @Inject
    ExceptionHandler exceptionHandler;
    @Inject
    StrategySharesProperties strategySharesProperties;

    List<Long> timers = new ArrayList<>();

    /**
     * Ищем кандидатов на покупку.
     * <p>
     * Берем спреды среди всех акций
     * и оставляем те где спред больше параметра robot.strategy.spread.percent.
     *
     * @return первые найденные без таймера акции.
     */
    @Override
    public Uni<List<String>> startFindFigi() {
        eventLogger.log("Ищем акции...");

        List<String> exchanges = exchangeService.getExchangesOpenNow();
        Uni<List<Share>> sharesUni = sharesService.getShares(exchanges);
        Uni<List<String>> firstFigi = sharesUni
                .invoke(shares -> {
                    eventLogger.log(String.format("Получено %d акций с бирж %s", shares.size(), exchanges));
                    createTimers(shares, strategySharesProperties.countOneMinute());
                })
                .onItem()
                .transformToUni(shares -> findFirstFigi(shares, strategySharesProperties.countOneMinute()));

        return firstFigi;
    }

    @Override
    public void stopTimers() {
        if (!timers.isEmpty()) {
            timers.forEach(t -> vertx.cancelTimer(t));
            timers = new ArrayList<>();
            eventLogger.log("Остановлены таймеры поиска акций");
        }
    }

    /**
     * Найти первые подходящие акции, поиск без таймера.
     *
     * @param sharesAll
     * @param endIndex
     * @return
     */
    private Uni<List<String>> findFirstFigi(List<Share> sharesAll, int endIndex) {
        endIndex = Math.min(sharesAll.size(), endIndex);
        List<Share> shares = sharesAll.subList(0, endIndex);
        eventLogger.log(String.format("Отбираем подходящие акции среди первых %d", shares.size()));
        return findFigi(shares);
    }

    /**
     * Создать таймеры для поиска акций.
     *
     * @param sharesAll     все акции
     * @param countOneTimer количество обрабатываемых акций в одном таймере
     */
    private void createTimers(List<Share> sharesAll, int countOneTimer) {
        int countTimers = sharesAll.size() / countOneTimer;
        eventLogger.log("Всего будет таймеров поиска акций " + countTimers);
        for (int i = 1; i <= countTimers; i++) {
            int startIndex = i * countOneTimer;
            int endIndex = Math.min((i + 1) * countOneTimer, sharesAll.size());
            createTimer(sharesAll, i, startIndex, endIndex);
        }
    }

    /**
     * Создать таймер для поиска акций.
     *
     * @param sharesAll
     * @param i          индекс таймера
     * @param startIndex
     * @param endIndex
     */
    private void createTimer(List<Share> sharesAll, int i, int startIndex, int endIndex) {
        List<Share> sharesOneTimer = sharesAll.subList(startIndex, endIndex);
        long millis = TimeUnit.MINUTES.toMillis(i);
        long timerId = vertx.setTimer(millis, aLong -> {
            eventLogger.log(String.format("Timer %d. Отбираем подходящие акции среди %d-%d", i, startIndex, endIndex));
            findFigi(sharesOneTimer);
        });
        timers.add(timerId);
    }

    /**
     * Поиск подходящих акций и отправка их в eventBus.
     *
     * @param shares список акций среди которых искать.
     * @return найденные figi подходящих акций
     */
    private Uni<List<String>> findFigi(List<Share> shares) {
        shares = filterShares(shares);
        Uni<List<String>> figisFind = spreadService.calcSpreads(shares)
                .filter(spreadDecision::isAppropriate)
                .map(Spread::getFigi)
                .collect().asList();

        figisFind.subscribe().with(figi -> {
            if (figi == null || figi.isEmpty()) {
                eventLogger.log("Пока не нашли продходящие акции. Ждем поиск следующих по таймеру через минуту.");
            } else {
                eventLogger.log("Найдены акции", figi);
                // todo сделать списком отправление
                bus.send(NEW_FIGI, figi.stream().collect(Collectors.joining(",")));
            }
        });
        return figisFind
                .onFailure()
                .invoke(throwable -> exceptionHandler.handle(throwable));
    }


    private List<Share> filterShares(List<Share> shares){
        if (strategySharesProperties.tickersExclude().isPresent()) {
            List<String> tickersExclude = strategySharesProperties.tickersExclude().get();
            shares = shares.stream()
                    .filter(share -> !tickersExclude.contains(share.getTicker()))
                    .collect(Collectors.toList());
        }
        return shares;
    }
}
