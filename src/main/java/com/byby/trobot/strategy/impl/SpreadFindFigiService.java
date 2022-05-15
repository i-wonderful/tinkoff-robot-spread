package com.byby.trobot.strategy.impl;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.service.impl.ExchangeService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.service.impl.SpreadService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    ApplicationProperties properties;

    @Inject
    StrategyCacheManager cacheManager;

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

        int countOneTimer = 200;

        List<String> exchanges = exchangeService.getExchangesOpenNow();
        List<Share> sharesAll = sharesService.getShares(List.of("SPB"));// TODO for testing
        eventLogger.log(String.format("Получено %d акций с бирж %s", sharesAll.size(), exchanges));

        int countTimers = sharesAll.size() / countOneTimer + 1;
        log.info(">>> Count Timers: " + countTimers);

        Uni<List<String>> firstFigi = findFirst(sharesAll, countOneTimer);

        for (int i = 1; i < countTimers; i++) {
            boolean isLastTimer = (i == (countTimers - 1));
            int startIndex = i * countOneTimer;
            int endIndex = isLastTimer ? sharesAll.size() : (i + 1) * countOneTimer;

            List<Share> sharesOneTimer = sharesAll.subList(startIndex, endIndex);
            runTimer(sharesOneTimer, i, isLastTimer, startIndex, endIndex);
        }

        return firstFigi;
    }

    private Uni<List<String>> findFirst(List<Share> sharesAll, int endIndex){
        List<Share> shares = sharesAll.subList(0, endIndex);
        eventLogger.log(String.format("Отбираем подходящие акции среди пенвых %d", endIndex));
        return findFigi(shares);
    }

    private void runTimer(List<Share> shares, int i, boolean isLastTimer, int startIndex, int endIndex) {
        long millis = TimeUnit.MINUTES.toMillis(i);
        vertx.setTimer(millis, aLong -> {
            eventLogger.log(String.format("Timer %d. Отбираем подходящие акции среди %d-%d", i, startIndex, endIndex));
            log.info(">>> Timer " + LocalTime.now() + ' ' + millis);
            findFigi(shares);

//            if (isLastTimer) {
//                log.info(">>>> Last Timer >>>");
//                // todo оповестить об окончании глобального поиска
//            }
        });
    }

    private Uni<List<String>> findFigi(List<Share> shares) {
        return spreadService.getSpreads(shares)
                .filter(spread -> properties.getRobotSpreadPercent() <= spread.getPercent())
                .map(Spread::getFigi)
                .collect().asList()
                .onItem()
                .transformToUni(figi -> cacheManager.addFigi(figi))
                .invoke(() -> {
                    bus.send("NEW_FIGI", "");
                });
    }

}
