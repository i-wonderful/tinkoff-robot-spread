package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.service.impl.SpreadService;
import com.byby.trobot.strategy.FindFigiService;
import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    SharesService sharesService;
    @Inject
    SpreadService spreadService;
    @Inject
    SpreadDecision spreadDecision;

    @ConfigProperty(name = "robot.strategy.shares.count.one.minute")
    int sharesCountOneMinute;

    List<Long> timers = new ArrayList<>();

    /**
     * Ищем кандидатов на покупку.
     * <p>
     * Берем спреды среди всех акций
     * и оставляем те где спред больше параметра robot.strategy.spread.percent.
     *
     * @return
     */
    @Override
    public Uni<List<String>> findFigi() {
        eventLogger.log("Ищем акции...");

        List<String> exchanges = exchangeService.getExchangesOpenNow();
        Uni<List<Share>> sharesUni = sharesService.getShares(exchanges);
        Uni<List<String>> firstFigi = sharesUni
                .invoke(shares -> {
                    eventLogger.log(String.format("Получено %d акций с бирж %s", shares.size(), exchanges));
                    runTimers(shares, sharesCountOneMinute);
                })
                .onItem()
                .transformToUni(shares -> findFirstFigi(shares, sharesCountOneMinute));

        return firstFigi;
    }

    @Override
    public void stopTimers() {
        timers.forEach(t -> vertx.cancelTimer(t));
        eventLogger.log("Остановлены таймеры поиска акций");
    }

    private Uni<List<String>> findFirstFigi(List<Share> sharesAll, int endIndex) {
        endIndex = Math.min(sharesAll.size(), endIndex);
        List<Share> shares = sharesAll.subList(0, endIndex);
        eventLogger.log(String.format("Отбираем подходящие акции среди первых %d", endIndex));
        return findFigi(shares);
    }

    /**
     * Создать таймеры для поиска акций.
     *
     * @param shares
     * @param countOneTimer
     */
    private void runTimers(List<Share> shares, int countOneTimer) {
        int countTimers = shares.size() / countOneTimer;
        eventLogger.log("Всего будет таймеров поиска акций " + countTimers);
        for (int i = 1; i <= countTimers; i++) {
            int startIndex = i * countOneTimer;
            int endIndex = Math.min((i + 1) * countOneTimer, shares.size());

            List<Share> sharesOneTimer = shares.subList(startIndex, endIndex);
            runTimer(sharesOneTimer, i, startIndex, endIndex);
        }
    }

    private void runTimer(List<Share> shares, int i, int startIndex, int endIndex) {
        long millis = TimeUnit.MINUTES.toMillis(i);
        long timerId = vertx.setTimer(millis, aLong -> {
            eventLogger.log(String.format("Timer %d. Отбираем подходящие акции среди %d-%d", i, startIndex, endIndex));
            findFigi(shares);
        });
        timers.add(timerId);
    }

    /**
     * Поиск подходящих акций и отправка их в eventBus
     *
     * @param shares список акций среди которых искать.
     * @return найденные figi подходящих акций
     */
    private Uni<List<String>> findFigi(List<Share> shares) {
        Uni<List<String>> figisFind = spreadService.getSpreads(shares)
                .filter(spreadDecision::isAppropriate)
                .map(Spread::getFigi)
                .collect().asList();

        figisFind.subscribe().with(figi -> {
            if (figi == null || figi.isEmpty()) {
                eventLogger.log("Пока не нашли продходящие акции. Ждем поиск следующих по таймеру.");
            } else {
                eventLogger.log("Найдены акции", figi);
                // todo сделать списком отправление
                bus.send(GlobalBusAddress.NEW_FIGI, figi.stream().collect(Collectors.joining(",")));
            }
        });
        return figisFind;
    }



}
