package com.byby.trobot.controller.dto;

import com.byby.trobot.db.entity.OrderDone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;

public class RobotSessionDto {

    private String accountId;

    private LocalDateTime startRobot;

    private LocalDateTime endRobot;

    private List<OrderDoneDto> orderDones;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getStartRobot() {
        return startRobot;
    }

    public void setStartRobot(LocalDateTime startRobot) {
        this.startRobot = startRobot;
    }

    public LocalDateTime getEndRobot() {
        return endRobot;
    }

    public void setEndRobot(LocalDateTime endRobot) {
        this.endRobot = endRobot;
    }

    public List<OrderDoneDto> getOrderDones() {
        return orderDones;
    }

    public void setOrderDones(List<OrderDoneDto> orderDones) {
        this.orderDones = orderDones;
    }
}
