package com.byby.trobot.db.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "robot_session")
public class RobotSession extends PanacheEntity {

    @Column(name = "account_id", length = 100)
    String accountId;

    @Column(name = "start_robot", nullable = false)
    LocalDateTime startRobot;

    @Column(name = "end_robot")
    LocalDateTime endRobot;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE})
    List<OrderDone> orders;

    public LocalDateTime getStartRobot() {
        return startRobot;
    }

    public void setStartRobot(LocalDateTime start) {
        this.startRobot = start;
    }

    public LocalDateTime getEndRobot() {
        return endRobot;
    }

    public void setEndRobot(LocalDateTime end) {
        this.endRobot = end;
    }

    public List<OrderDone> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDone> orders) {
        this.orders = orders;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
