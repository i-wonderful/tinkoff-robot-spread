package com.byby.trobot.db.repository;

import com.byby.trobot.db.entity.RobotSession;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RobotSessionRepository implements PanacheRepository<RobotSession> {
}
