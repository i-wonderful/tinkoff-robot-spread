package com.byby.trobot.service.impl;

import com.byby.trobot.config.RobotProperties;
import com.byby.trobot.controller.dto.ExchangeDto;
import com.byby.trobot.controller.exception.BusinessException;
import com.byby.trobot.service.ExchangeService;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.TradingDay;
import ru.tinkoff.piapi.contract.v1.TradingSchedule;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToInstant;

/**
 * Получение информации о биржах.
 */
@ApplicationScoped
public class ExchangeServiceImpl implements ExchangeService {
    private static final Logger log = LoggerFactory.getLogger(ExchangeServiceImpl.class);

    private RobotProperties robotProperties;
    private InstrumentsService instrumentsService;

    public ExchangeServiceImpl(InvestApi api, RobotProperties robotProperties) {
        this.instrumentsService = api.getInstrumentsService();
        this.robotProperties = robotProperties;
    }

    /**
     * Информация о биржах в данный момент.
     * Открыты/закрыты, когда откроются.
     * Список бирж с которыми работаем берется из настроек.
     *
     * @return список информации о биржах в данный момент
     */
    @Override
    public List<ExchangeDto> getExchangesInfo() {
        return getExchangesInfo(robotProperties.exchangeNames());
    }

    /**
     * Открытые в данный момент биржи.
     *
     * @return список открытых бирх
     */
    @Override
    public List<String> getExchangesOpenNow() {
        return getExchangesInfo()
                .stream()
                .filter(exc -> exc.isOpen())
                .map(ExchangeDto::getName)
                .collect(Collectors.toList());
    }

    /**
     * Информация по всем биржам
     *
     * @param exchanges
     * @return
     */
    private List<ExchangeDto> getExchangesInfo(List<String> exchanges) {
        return exchanges.stream()
                .map(exchange -> {
                    // todo make async
                    TradingSchedule tradingSchedule = instrumentsService
                            .getTradingScheduleSync(exchange, Instant.now(), Instant.now().plus(6, ChronoUnit.DAYS));
                    return getExchangeInfo(tradingSchedule);
                })
                .collect(Collectors.toList());
    }

    /**
     * Информация о бирже.
     *
     * @param tsh расписание
     * @return
     */
    private ExchangeDto getExchangeInfo(TradingSchedule tsh) {
        List<TradingDay> tradingDays = tsh.getDaysList();
        TradingDay today = tradingDays.get(0);

        ExchangeDto exchangeDto = new ExchangeDto();
        exchangeDto.setName(tsh.getExchange());
        exchangeDto.setTradingDay(today.getIsTradingDay());
        exchangeDto.setOpen(isOpenNow(today));
        timeBefore timeBefore = getHoursAndMinutesBeforeOpen(tradingDays);
        exchangeDto.setMinutesBeforeOpen(timeBefore.minutes);
        exchangeDto.setHoursBeforeOpen(timeBefore.hour);

        return exchangeDto;
    }

    /**
     * Открыта ли в данный момент
     */
    private boolean isOpenNow(TradingDay today) {
        Instant now = Instant.now();
        Timestamp tsEnd = today.getEndTime();
        Timestamp tsStart = today.getStartTime();

        return now.isAfter(timestampToInstant(tsStart)) &&
                now.isBefore(timestampToInstant(tsEnd));
    }

    /**
     * Поччитать время до открытыя.
     *
     * @param tradingDays расписание
     * @return часы и минуты до открытия
     */
    private timeBefore getHoursAndMinutesBeforeOpen(List<TradingDay> tradingDays) {
        if (tradingDays == null || tradingDays.isEmpty()) {
            throw new BusinessException("Error get trading days");
        }
        TradingDay firstWorkDay = findNextWorkDay(tradingDays);

        timeBefore timeBefore = calcTimeBefore(firstWorkDay);
        if (timeBefore.hour < 0) {
            // сегодня биржа уже закрылась.
            // считаем время до ближайшего рабочего дня
            TradingDay nextWorkDay = findNextWorkDay(tradingDays.subList(1, tradingDays.size()));
            timeBefore = calcTimeBefore(nextWorkDay);
        }
        return timeBefore;
    }

    private timeBefore calcTimeBefore(TradingDay tradingDay) {
        Instant now = Instant.now();
        long h = ChronoUnit.HOURS.between(now, timestampToInstant(tradingDay.getStartTime()));
        long m = ChronoUnit.MINUTES.between(now.plus(h, ChronoUnit.HOURS), timestampToInstant(tradingDay.getStartTime()));
        return new timeBefore((int) h, (int) m);
    }

    private TradingDay findNextWorkDay(List<TradingDay> tradingDays) {
        return tradingDays.stream()
                .filter(TradingDay::getIsTradingDay)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Не найдено рабочих дней в расписании"));
    }

    class timeBefore {
        int hour;
        int minutes;

        public timeBefore(int hour, int minutes) {
            this.hour = hour;
            this.minutes = minutes;
        }
    }
}
