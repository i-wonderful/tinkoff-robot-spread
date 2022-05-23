package com.byby.trobot.controller.dto.mapper;

import com.byby.trobot.controller.dto.OrderStateDto;
import com.byby.trobot.service.impl.SharesServiceImpl;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;

@ApplicationScoped
public class OrderMapper {
    @Inject
    SharesServiceImpl sharesService;

    public Uni<List<OrderStateDto>> toDtoUni(List<OrderState> orderStates) {
        if (orderStates == null || orderStates.isEmpty()) {
            return Uni.createFrom().item(Collections.emptyList());
        }
        return Uni.join().all(
                                orderStates.stream()
                                        .map(this::toDto)
                                        .collect(Collectors.toList()))
                        .andCollectFailures()
                        .onItem()
                        .transform(orderStateDtos ->
                                orderStateDtos.stream()
                                        .sorted(Comparator.comparing(OrderStateDto::getTicker))
                                        .collect(Collectors.toList()));
    }

    public Uni<OrderStateDto> toDto(OrderState orderState) {
        return sharesService.findTickerByFigi(orderState.getFigi())
                .onItem()
                .transform(ticker -> {
                    OrderStateDto dto = new OrderStateDto();
                    dto.setOrderId(orderState.getOrderId());
                    dto.setStatus(getStatus(orderState.getExecutionReportStatus()));
                    dto.setDirection(getDirectionRus(orderState.getDirection()));
                    dto.setFigi(orderState.getFigi());
                    dto.setInitialPrice(moneyValueToBigDecimal(orderState.getInitialSecurityPrice()));
                    dto.setCurrency(orderState.getCurrency());
                    dto.setTicker(ticker);
                    return dto;
                });
    }

    public OrderStateDto toDto(PostOrderResponse order) {
        if (order == null) {
            return null;
        }
        OrderStateDto dto = new OrderStateDto();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(getStatus(order.getExecutionReportStatus()));
        dto.setDirection(getDirectionRus(order.getDirection()));
        dto.setFigi(order.getFigi());
        dto.setInitialPrice(moneyValueToBigDecimal(order.getInitialSecurityPrice()));
        dto.setCurrency(order.getInitialSecurityPrice().getCurrency());
        dto.setTicker(sharesService.findTickerByFigiSync(order.getFigi()));
        return dto;
    }

    private String getStatus(OrderExecutionReportStatus status) {
        switch (status) {
            case EXECUTION_REPORT_STATUS_NEW:
                return "Новая";
            case EXECUTION_REPORT_STATUS_FILL:
                return "Исполнена";
            case EXECUTION_REPORT_STATUS_REJECTED:
                return "Отклонена";
            case EXECUTION_REPORT_STATUS_CANCELLED:
                return "Отменена пользователем";
            case EXECUTION_REPORT_STATUS_PARTIALLYFILL:
                return "Частично исполнена";
            case EXECUTION_REPORT_STATUS_UNSPECIFIED:
                return "none";
            default:
                return "";
        }
    }

    public static String getDirectionRus(OrderDirection direction) {
        switch (direction) {
            case ORDER_DIRECTION_BUY:
                return "Покупка";
            case ORDER_DIRECTION_SELL:
                return "Продажа";
            case ORDER_DIRECTION_UNSPECIFIED:
                return "Значение не указано";
            default:
                return "";
        }
    }
}
