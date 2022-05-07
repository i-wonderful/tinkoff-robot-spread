package com.byby.trobot.service;

import com.byby.trobot.common.InvestUtil;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.TradingDay;
import ru.tinkoff.piapi.contract.v1.TradingSchedule;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;
import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;

@RequestScoped
public class ExchangeService {
    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);

    @Inject
    InvestApi api;

    /**
     * Проверяет открыты ли биржи в данный момент
     * @param exchanges биржи для проверки
     * @return список открытых бирж
     */
    public List<String> isOpenNow(List<String> exchanges) {
        return exchanges.stream()
                .filter(exchange -> {
                    TradingSchedule tradingSchedule = api.getInstrumentsService()
                            .getTradingScheduleSync(exchange, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS));
                    return isOpenNow(tradingSchedule);
                }).collect(Collectors.toList());
    }


    private boolean isOpenNow(TradingSchedule tsh) {
        String exchange = tsh.getExchange();
        List<TradingDay> tradingDays = tsh.getDaysList();

        for (TradingDay tradingDay : tradingDays) {

            Instant now = Instant.now();
            Timestamp tsEnd = tradingDay.getEndTime();
            Timestamp tsStart = tradingDay.getStartTime();
            boolean isBetween = now.isAfter(timestampToInstant(tsStart)) &&
                    now.isBefore(timestampToInstant(tsEnd));

            var date = timestampToString(tradingDay.getDate());
            var startDate = timestampToString(tradingDay.getStartTime());
            var endDate = timestampToString(tradingDay.getEndTime());
            if (tradingDay.getIsTradingDay()) {
                log.info("расписание торгов для площадки {}. Дата: {},  открытие: {}, закрытие: {}", exchange, date, startDate, endDate);
                return isBetween;
            } else {
                log.info("расписание торгов для площадки {}. Дата: {}. Выходной день", exchange, date);
                return false;
            }
        }

        return false;
    }
}
