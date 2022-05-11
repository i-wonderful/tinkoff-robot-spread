package com.byby.trobot.dto.mapper;

import com.byby.trobot.dto.OrderStateDto;
import com.byby.trobot.service.impl.SharesService;
import ru.tinkoff.piapi.contract.v1.OrderState;

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
        String status = null;
        switch (orderState.getExecutionReportStatus()) {
            case EXECUTION_REPORT_STATUS_NEW: status = "Новая"; break;
            case EXECUTION_REPORT_STATUS_FILL: status = "Исполнена"; break;
            case EXECUTION_REPORT_STATUS_REJECTED: status = "Отклонена"; break;
            case EXECUTION_REPORT_STATUS_CANCELLED: status = "Отменена пользователем"; break;
            case EXECUTION_REPORT_STATUS_PARTIALLYFILL: status = "Частично исполнена"; break;
            case EXECUTION_REPORT_STATUS_UNSPECIFIED: status = "none"; break;
        }
        dto.setStatus(status);
        String direction = null;
        switch (orderState.getDirection()) {
            case ORDER_DIRECTION_BUY: direction = "Покупка"; break;
            case ORDER_DIRECTION_SELL: direction = "Продажа"; break;
            case ORDER_DIRECTION_UNSPECIFIED: direction = "Значение не указано"; break;
        }
        dto.setDirection(direction);
        dto.setFigi(orderState.getFigi());
        dto.setInitialPrice(moneyValueToBigDecimal(orderState.getInitialSecurityPrice()));
        dto.setCurrency(orderState.getCurrency());
        dto.setTicker(sharesService.findTickerByFigi(orderState.getFigi()));
        return dto;
    }
}
