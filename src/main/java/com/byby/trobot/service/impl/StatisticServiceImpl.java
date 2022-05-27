package com.byby.trobot.service.impl;

import com.byby.trobot.cache.AppCache;
import com.byby.trobot.controller.dto.RobotSessionDto;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.controller.handler.ExceptionHandler;
import com.byby.trobot.db.entity.OrderDone;
import com.byby.trobot.db.entity.RobotSession;
import com.byby.trobot.db.mapper.OrderDoneMapper;
import com.byby.trobot.db.mapper.RobotSessionMapper;
import com.byby.trobot.db.repository.OrderDoneRepository;
import com.byby.trobot.db.repository.RobotSessionRepository;
import com.byby.trobot.service.StatisticService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderTrades;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class StatisticServiceImpl implements StatisticService {
    private static final Logger log = LoggerFactory.getLogger(StatisticServiceImpl.class);

    @Inject
    OrderDoneRepository orderDoneRepository;
    @Inject
    RobotSessionRepository robotSessionRepository;
    @Inject
    OrderDoneMapper orderDoneMapper;
    @Inject
    AppCache appCache;
    @Inject
    ExceptionHandler exceptionHandler;

    Long robotId; // todo to cache

    @Override
    public Uni<Void> start() {
        log.info(">>> Statistic start");
        RobotSession robotSession = new RobotSession();
        robotSession.setStartRobot(LocalDateTime.now());
//        robotSession.setAccountId(appCache.getAccountId()); // todo

        return Panache.withTransaction(() -> robotSessionRepository.persist(robotSession))
                .onItem()
                .invoke(rs -> {
                    robotId = rs.id;
                    log.info(">>> Statistic start save success");
                })
                .onFailure()
                .invoke(throwable -> exceptionHandler.handle(throwable, "Ошибка старта статистики"))
                .replaceWithVoid();
    }

    @ActivateRequestContext
    @Override
    public Uni<Void> stop() {
        if (robotId == null) {
            return Uni.createFrom().voidItem();
        }
        log.info(">>> Statistic stop");
        return robotSessionRepository.findById(robotId) // todo разобраться с кешами
                .onItem()
                .transformToUni(rs -> {
                    this.robotId = null;
                    if (rs.getEndRobot() == null) {
                        rs.setEndRobot(LocalDateTime.now());
                    }
                    return robotSessionRepository.persistAndFlush(rs);
                })
                .onItem()
                .invoke(() -> log.info(">>> Statistic stop save success"))
                .onFailure()
                .retry()
                .atMost(2)
                .onFailure()
                .invoke(throwable -> exceptionHandler.handle(throwable, "Ошибка сохранения статистики"))
                .replaceWithVoid();
    }

    @Override
    public Multi<RobotSessionDto> getAll() {
        return robotSessionRepository.findAll(Sort.descending("startRobot"))
                .stream()
                .map(RobotSessionMapper::toDto);
    }

    @Override
    @ActivateRequestContext
    public Uni<Void> save(OrderTrades orderTrades) {
        log.info(">>> Statistic save 1, robotId={} ot={}", robotId, orderTrades);
        return Panache.withTransaction(() -> robotSessionRepository.findById(robotId)
                //.emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem()
                .transformToUni(rs -> {
                    OrderDone orderDone = orderDoneMapper.toEntity(orderTrades);
                    orderDone.setRobotSession(rs);
                    log.info(">>> Statistic save 2");
                    return orderDone.persistAndFlush();
                }))
                .onItem()
                .invoke((t) -> log.info(">>> Statistic save success , orderDone={}", t))
                .onFailure()
                .retry()
                .atMost(2)
                .onFailure()
                .invoke(throwable -> exceptionHandler.handle(throwable, "Ошибка сохранения статистики", orderTrades.getFigi()))
                .replaceWithVoid();
    }
}
