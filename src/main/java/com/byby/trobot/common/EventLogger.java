package com.byby.trobot.common;

import com.byby.trobot.dto.OrderStateDto;
import com.byby.trobot.dto.codec.OrderStateDtoCodec;
import com.byby.trobot.dto.mapper.OrderMapper;
import com.byby.trobot.service.impl.SharesService;
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

/**
 * Логгирование событий.
 */
@ApplicationScoped
public class EventLogger {
    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);
    private static final String TEMPLATE_ADD_ORDER_BUY = "[%s] Выставлена лимитная заявка на покупку по цене %f, orderId=%s";
    private static final String TEMPLATE_ADD_ORDER_SELL = "[%s] Выставлена лимитная заявка на продажу по цене %f, orderId=%s";
    private static final String TEMPLATE_CANCEL_ORDER = "[%s] Отменена заявка на %s. orderId=%s";

    @Inject
    SharesService sharesService;

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
    public Uni log(String message) {
        log.info(message);
        bus.publish(LOG, message);
        return Uni.createFrom().voidItem();
    }

    /**
     * Лог сообщения, с выводом тикера акции
     *
     * @param message
     * @param figi
     * @return
     */
    public Uni log(String message, String figi) {
        String ticker = sharesService.findTickerByFigiSync(figi);
        log.info("[" + ticker + "] " + message + ", figi: " + figi);
        bus.publish(LOG, "[" + ticker + "] " + message);
        return Uni.createFrom().voidItem();
    }

    /**
     * Логи сообщения с выводом списка тикеров акций
     *
     * @param message
     * @param figis
     * @return
     */
    public Uni log(String message, List<String> figis) {
        String tickers = figis.stream()
                .map(figi -> sharesService.findTickerByFigiSync(figi))
                .collect(Collectors.joining(","));
        log.info("[" + tickers + "] " + message);
        bus.publish(LOG, "[" + tickers + "] " + message);
        return Uni.createFrom().voidItem();
    }


    /**
     * Оправить в eventBus саму заявку для вывода в ui
     */
    public Uni uiOrderList(OrderStateDto dto) {
        bus.publish(LOG_ORDER, dto, new DeliveryOptions().setCodecName(OrderStateDtoCodec.NAME));
        return Uni.createFrom().voidItem();
    }

    /**
     * Лог: добавление новой заявки.
     */
    public Uni logPostOrder(PostOrderResponse response) {
        OrderStateDto dto = orderMapper.toDto(response);
        dto.setUiAction("ADD");
        uiOrderList(dto);

        String template = "%s %f %s";
        if(response.getDirection().equals(ORDER_DIRECTION_BUY)) {
            template = TEMPLATE_ADD_ORDER_BUY;
        } else if (response.getDirection().equals(ORDER_DIRECTION_SELL)) {
            template = TEMPLATE_ADD_ORDER_SELL;
        }
        String messageLog = String.format(template, dto.getTicker(), dto.getInitialPrice(), dto.getOrderId());
        bus.publish(LOG, messageLog);
        return Uni.createFrom().voidItem();
    }

    /**
     * Лог: отмена заявки.
     *
     * @param orderId
     * @param figi
     * @param orderDirection
     * @return
     */
    public Uni logOrderCancel(String orderId, String figi, OrderDirection orderDirection) {
        String ticker = sharesService.findTickerByFigiSync(figi);
        String message = String.format(TEMPLATE_CANCEL_ORDER, ticker, OrderMapper.getDirectionRus(orderDirection), orderId);

        // вывод текстовых сообщений о событии
        bus.publish(LOG, message);
        log.info(message);

        // убрать из таблицы ордеров в ui
        OrderStateDto dto = new OrderStateDto();
        dto.setOrderId(orderId);
        dto.setUiAction("REMOVE");
        uiOrderList(dto);

        return Uni.createFrom().voidItem();
    }

    public void logError(Exception exception) {
        log.error(">>> !!!!!!!!!!!!!!!!!!!!!!", exception);
        bus.publish(GlobalBusAddress.LOG, ">>> Exception " + exception.getMessage());
    }
}
