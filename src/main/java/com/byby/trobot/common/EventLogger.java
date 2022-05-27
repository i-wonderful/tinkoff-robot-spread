package com.byby.trobot.common;

import com.byby.trobot.controller.dto.OrderStateDto;
import com.byby.trobot.controller.dto.codec.OrderStateDtoCodec;
import com.byby.trobot.controller.dto.mapper.OrderMapper;
import com.byby.trobot.service.impl.SharesServiceImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ru.tinkoff.piapi.contract.v1.OrderDirection.*;

import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

import static com.byby.trobot.common.GlobalBusAddress.LOG;
import static com.byby.trobot.common.GlobalBusAddress.LOG_ORDER;
import static com.byby.trobot.controller.dto.OrderStateDto.OrderStatus.*;
/**
 * Логгирование событий.
 */
@ApplicationScoped
public class EventLogger {
    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);
    private static final String TEMPLATE_ORDER_ADD_BUY = "[%s] Выставлена лимитная заявка на покупку по цене %f, orderId=%s";
    private static final String TEMPLATE_ORDER_ADD_SELL = "[%s] Выставлена лимитная заявка на продажу по цене %f, orderId=%s";
    private static final String TEMPLATE_ORDER_CANCEL = "[%s] Отменена заявка на %s. orderId=%s";
    private static final String TEMPLATE_ORDER_DONE = "[%s] Заявка исполнена на %s. orderId=%s";

    @Inject
    SharesServiceImpl sharesService;

    @Inject
    OrderMapper orderMapper;

    @Inject
    EventBus bus;

    /**
     * Лог простого сообщения.
     *
     * @param message
     * @return
     */
    public void log(String message) {
        log.info(message);
        bus.publish(LOG, message);
    }

    /**
     * Лог сообщения, с выводом тикера акции.
     *
     * @param message
     * @param figi
     * @return
     */
    public void log(String message, String figi) {
        sharesService.findTickerByFigi(figi)
                .subscribe()
                .with(ticker -> {
                    log.info("[" + ticker + "] " + message + ", figi: " + figi);
                    bus.publish(LOG, "[" + ticker + "] " + message);
                });
    }

    /**
     * Логирование сообщения с выводом списка тикеров акций.
     *
     * @param message
     * @param figis
     * @return
     */
    public void log(String message, List<String> figis) {
        List<Uni<String>> tickerUnis = figis.stream()
                .map(figi -> sharesService.findTickerByFigi(figi))
                .collect(Collectors.toList());

        Uni.join().all(tickerUnis).andCollectFailures()
                .subscribe()
                .with(tickers -> {
                    log.info(tickers + " " + message);
                    bus.publish(LOG,  tickers + " " + message);
                });
    }


    /**
     * Оправить в eventBus саму заявку для вывода в ui
     */
    private void uiOrderList(OrderStateDto dto) {
        bus.publish(LOG_ORDER, dto, new DeliveryOptions().setCodecName(OrderStateDtoCodec.NAME));
    }

    private void uiOrderListRemove(String orderId, OrderStateDto.OrderStatus status) {
        OrderStateDto dto = new OrderStateDto();
        dto.setOrderId(orderId);
        dto.setOrderStatus(CANCEL);
        dto.setOrderStatus(status);
        uiOrderList(dto);
    }

    /**
     * Лог: добавление новой заявки.
     */
    public void logPostOrder(PostOrderResponse response) {
        if (response == null) {
            return;
        }
        OrderStateDto dto = orderMapper.toDto(response);
        dto.setOrderStatus(NEW);
        uiOrderList(dto);

        String template = "%s %f %s";
        if (response.getDirection().equals(ORDER_DIRECTION_BUY)) {
            template = TEMPLATE_ORDER_ADD_BUY;
        } else if (response.getDirection().equals(ORDER_DIRECTION_SELL)) {
            template = TEMPLATE_ORDER_ADD_SELL;
        }
        String messageLog = String.format(template, dto.getTicker(), dto.getInitialPrice(), dto.getOrderId());
        bus.publish(LOG, messageLog);
    }

    /**
     * Лог: отмена заявки.
     *
     * @param orderId
     * @param figi
     * @param orderDirection
     * @return
     */
    public void logOrderCancel(String orderId, String figi, OrderDirection orderDirection) {
        logOrderAndUiRemove(TEMPLATE_ORDER_CANCEL, orderId, figi, orderDirection, CANCEL);
    }

    /**
     * Лог: заявка исполнена.
     *
     * @param orderId
     * @param figi
     * @param orderDirection
     * @return
     */
    public void logOrderDone(String orderId, String figi, OrderDirection orderDirection) {
        logOrderAndUiRemove(TEMPLATE_ORDER_DONE, orderId, figi, orderDirection, DONE);
    }

    /**
     *
     * @param figi
     */
    public void logCurrentRunFigi(List<String> figi) {
        List<Uni<String>> tickersUni = figi.stream()
                .map(sharesService::findTickerByFigi)
                .collect(Collectors.toList());

        Uni.join().all(tickersUni)
                .andCollectFailures()
                .onItem()
                .transform(tickers -> tickers.stream().collect(Collectors.joining(",")))
                .subscribe()
                .with(tickers -> bus.publish(GlobalBusAddress.LOG_CURRENT_RUN_TICKERS, tickers));
    }

    /**
     * Вывод в лог и удалить ордер из таблицы в ui.
     */
    private void logOrderAndUiRemove(String template, String orderId, String figi, OrderDirection orderDirection, OrderStateDto.OrderStatus status) {
        sharesService.findTickerByFigi(figi)
                .subscribe()
                .with(ticker -> {
                    String message = String.format(template, ticker, OrderMapper.getDirectionRus(orderDirection), orderId);
                    // вывод текстовых сообщений о событии
                    bus.publish(LOG, message);
                    log.info(message);

                    // убрать из таблицы ордеров в ui
                    uiOrderListRemove(orderId, status);
                });
    }

    public void logError(String message) {
        log.warn(message);
        bus.publish(GlobalBusAddress.LOG_ERROR, message);
    }

    public void logError(Throwable exception) {
        log.error(">>> !!!!!!!!!!!!!!!!!!!!!!", exception);
        bus.publish(GlobalBusAddress.LOG_ERROR, ">>> Exception " + exception.getMessage());
    }

    public void logCritical(String message) {
        bus.publish(GlobalBusAddress.LOG_ERR_CRITICAL, message);
    }
}
