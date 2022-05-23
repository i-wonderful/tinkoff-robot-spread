package com.byby.trobot.db.mapper;

import com.byby.trobot.controller.dto.RobotSessionDto;
import com.byby.trobot.db.entity.RobotSession;

public class RobotSessionMapper {

    public static RobotSessionDto toDto(RobotSession entity){
        RobotSessionDto dto = new RobotSessionDto();
        dto.setAccountId(entity.getAccountId());
        dto.setStartRobot(entity.getStartRobot());
        dto.setEndRobot(entity.getEndRobot());
        dto.setOrderDones(OrderDoneMapper.toDto(entity.getOrderDones()));
        return dto;
    }
}
