package com.byby.trobot.service.impl;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.ExchangeOpenDto;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.TradingDay;
import ru.tinkoff.piapi.contract.v1.TradingSchedule;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;
import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;

/**
 * Операции с биржами
 */
@RequestScoped
public class ExchangeService {
    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);

    @Inject
    InvestApi api;

    @Inject
    ApplicationProperties properties;

    /**
     * Информация о биржах в данный момент. Открыты/закрыты, когда откроются.
     * Список бирж с которыми работаем берется из настроек.
     *
     * @return список открытых бирж
     */
    public List<ExchangeOpenDto> getExchangesInfoNow() {
        return getExchangesInfoNow(properties.getRobotExchangeNames());
    }

    /**
     * @return
     */
    public List<String> getExchangesOpenNow() {
        return getExchangesInfoNow().stream()
                .filter(exc -> exc.isOpen())
                .map(ExchangeOpenDto::getName)
                .collect(Collectors.toList());
    }

    private List<ExchangeOpenDto> getExchangesInfoNow(List<String> exchanges) {
        return exchanges.stream()
                .map(exchange -> {
                    TradingSchedule tradingSchedule = api.getInstrumentsService()
                            .getTradingScheduleSync(exchange, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS));
                    return isOpenNow(tradingSchedule);
                })
                .collect(Collectors.toList());
    }

    private ExchangeOpenDto isOpenNow(TradingSchedule tsh) {
        ExchangeOpenDto result = new ExchangeOpenDto();

        String exchange = tsh.getExchange();
        List<TradingDay> tradingDays = tsh.getDaysList();

        for (TradingDay tradingDay : tradingDays) {
            result.setName(exchange);
            result.setTradingDay(tradingDay.getIsTradingDay());

            var date = timestampToString(tradingDay.getDate());
            var startDate = timestampToString(tradingDay.getStartTime());
            var endDate = timestampToString(tradingDay.getEndTime());

            if (tradingDay.getIsTradingDay()) {

                Instant now = Instant.now();
                Timestamp tsEnd = tradingDay.getEndTime();
                Timestamp tsStart = tradingDay.getStartTime();
                boolean isOpen = now.isAfter(timestampToInstant(tsStart)) &&
                        now.isBefore(timestampToInstant(tsEnd));
                result.setOpen(isOpen);

                if (!isOpen) {
                    long h = ChronoUnit.HOURS.between(now, timestampToInstant(tradingDay.getStartTime()));
                    long m = ChronoUnit.MINUTES.between(now.plus(h, ChronoUnit.HOURS), timestampToInstant(tradingDay.getStartTime()));
                    result.setHoursBeforeOpen((int) h);
                    result.setMinutesBeforeOpen((int) m);
                }

                log.info("расписание торгов для площадки {}. Дата: {},  открытие: {}, закрытие: {}", exchange, date, startDate, endDate);

                return result;
            } else {
                result.setTradingDay(false);
                log.info("расписание торгов для площадки {}. Дата: {}. Выходной день", exchange, date);
                return result;
            }
        }

        return result;
    }
}
