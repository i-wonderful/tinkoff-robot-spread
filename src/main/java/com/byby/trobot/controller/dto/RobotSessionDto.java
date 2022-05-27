package com.byby.trobot.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RobotSessionDto {

    private String accountId;

    private LocalDateTime startRobot;

    private LocalDateTime endRobot;

    private BigDecimal balance;

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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
