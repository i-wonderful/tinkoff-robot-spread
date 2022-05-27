package com.byby.trobot.db.mapper;

import com.byby.trobot.controller.dto.RobotSessionDto;
import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.RobotSession;

import java.math.BigDecimal;
import java.util.List;

public class RobotSessionMapper {

    public static RobotSessionDto toDto(RobotSession entity) {
        RobotSessionDto dto = new RobotSessionDto();
        dto.setAccountId(entity.getAccountId());
        dto.setStartRobot(entity.getStartRobot());
        dto.setEndRobot(entity.getEndRobot());
        dto.setOrderDones(OrderDoneMapper.toDto(entity.getOrderDones()));
        dto.setBalance(calcBalance(entity.getOrderDones()));

        return dto;
    }

    private static BigDecimal calcBalance(List<OrderDone> orderDones) {
        return orderDones.stream()
                .map(od -> od.getPrice().multiply(BigDecimal.valueOf(od.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
