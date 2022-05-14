package com.byby.trobot.dto.mapper;

import com.byby.trobot.dto.OrderStateDto;
import com.byby.trobot.service.impl.SharesService;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;

@RequestScoped
public class OrderMapper {
    @Inject
    SharesService sharesService;

    public List<OrderStateDto> toDto(List<OrderState> orderStates) {
        return orderStates.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public OrderStateDto toDto(OrderState orderState) {
        OrderStateDto dto = new OrderStateDto();
        dto.setOrderId(orderState.getOrderId());
        dto.setStatus(getStatus(orderState.getExecutionReportStatus()));
        dto.setDirection(getDirection(orderState.getDirection()));
        dto.setFigi(orderState.getFigi());
        dto.setInitialPrice(moneyValueToBigDecimal(orderState.getInitialSecurityPrice()));
        dto.setCurrency(orderState.getCurrency());
        dto.setTicker(sharesService.findTickerByFigi(orderState.getFigi()));
        return dto;
    }

    public OrderStateDto toDto(PostOrderResponse order){
        OrderStateDto dto = new OrderStateDto();
        dto.setOrderId(order.getOrderId());
        dto.setStatus(getStatus(order.getExecutionReportStatus()));
        dto.setDirection(getDirection(order.getDirection()));
        dto.setFigi(order.getFigi());
        dto.setInitialPrice(moneyValueToBigDecimal(order.getInitialSecurityPrice()));
        dto.setCurrency(order.getInitialSecurityPrice().getCurrency());
        dto.setTicker(sharesService.findTickerByFigi(order.getFigi()));
        return dto;
    }

    private String getStatus(OrderExecutionReportStatus status){
        switch (status) {
            case EXECUTION_REPORT_STATUS_NEW: return  "Новая";
            case EXECUTION_REPORT_STATUS_FILL: return  "Исполнена";
            case EXECUTION_REPORT_STATUS_REJECTED: return  "Отклонена";
            case EXECUTION_REPORT_STATUS_CANCELLED: return  "Отменена пользователем";
            case EXECUTION_REPORT_STATUS_PARTIALLYFILL: return  "Частично исполнена";
            case EXECUTION_REPORT_STATUS_UNSPECIFIED: return "none";
            default: return "";
        }
    }

    private String getDirection(OrderDirection direction){
        switch (direction) {
            case ORDER_DIRECTION_BUY: return "Покупка";
            case ORDER_DIRECTION_SELL: return "Продажа";
            case ORDER_DIRECTION_UNSPECIFIED: return "Значение не указано";
            default: return "";
        }
    }
}
