package com.byby.trobot.service.impl;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.RobotSession;
import com.byby.trobot.db.mapper.OrderEntityMapper;
import com.byby.trobot.db.repository.OrderDoneRepository;
import com.byby.trobot.db.repository.RobotSessionRepository;
import com.byby.trobot.service.StatisticService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderTrades;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class StatisticServiceImpl implements StatisticService {
    @Inject
    OrderDoneRepository orderDoneRepository;
    @Inject
    RobotSessionRepository robotSessionRepository;
    @Inject
    OrderEntityMapper orderEntityMapper;
    @Inject
    AppCache appCache;

    @Transactional
    public Uni<Void> start() {
        RobotSession robotSession = new RobotSession();
        robotSession.setStartRobot(LocalDateTime.now());
        robotSession.setAccountId(appCache.getAccountId());

        return robotSessionRepository.persistAndFlush(robotSession)
                .onItem()
                .invoke(rs -> appCache.putRobotSessionId(rs.id))
                .onFailure()
                .invoke(throwable -> new CriticalException("Ошибка старта сохранения статистики"))
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> stop() {
        return robotSessionRepository.findById(appCache.getRobotSessionId())
                .onItem()
                .transformToUni(rs -> {
                    rs.setEndRobot(LocalDateTime.now());
                    return robotSessionRepository.persistAndFlush(rs);
                })
                .onFailure()
                .invoke(throwable -> new CriticalException("Ошибка сохранения статистики"))
                .replaceWithVoid();
    }

    @Override
    public Multi<RobotSession> getAll() {
        return robotSessionRepository.findAll().stream();
    }

    @Override
    public Uni<Void> save(OrderTrades orderTrades) {
        return robotSessionRepository.findById(appCache.getRobotSessionId())
                .onItem()
                .transformToUni(rs -> {
                    OrderDone orderDone = orderEntityMapper.toEntity(orderTrades);
                    orderDone.setRobotSession(rs);
                    return orderDone.persistAndFlush();
                })
                .onFailure()
                .invoke(throwable -> new CriticalException("Ошибка сохранения статистики"))
                .replaceWithVoid();
    }
}
