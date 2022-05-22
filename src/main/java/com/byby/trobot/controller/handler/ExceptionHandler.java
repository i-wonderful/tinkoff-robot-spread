package com.byby.trobot.controller.handler;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.service.StrategyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    private final static String CODE_ERROR_LIMIT = "80002";
    private final static String CODE_INTERNAL_ERROR = "70001";

    @Inject
    EventLogger eventLogger;

    @Inject
    StrategyManager strategyManager;

    public void handleCritical(Throwable throwable, String additionalInfo) {
        log.error(">>> Critical Error: " + throwable.getMessage() + " " + additionalInfo);
        eventLogger.logCritical(additionalInfo + " " + throwable.getMessage());
        handle(throwable);
    }

    public void handle(Throwable throwable) {
        if (throwable instanceof ApiRuntimeException) {
            handleException((ApiRuntimeException) throwable);
        } else if (throwable instanceof CriticalException) {
            handleException((CriticalException) throwable);
        } else {
            log.error("Oh no! We received a failure: " + throwable.getMessage());
        }
    }

    public void handleException(CriticalException e) {
        log.info(">>>> Handle CriticalException");
        eventLogger.logCritical(e.getMessage());
        eventLogger.log("Критическая ошибка. Останавливаем робота.");
        strategyStop();
        if (e.getSourceThrMessage() != null) {
            eventLogger.logCritical(e.getSourceThrMessage());
        }
    }

    public void handleException(ApiRuntimeException tinkoffEx) {
        if (CODE_ERROR_LIMIT.equals(tinkoffEx.getCode())) {
            eventLogger.logError(tinkoffEx.getMessage());
            eventLogger.log("Ошибка. Превышены лимиты. Останавливаем робота. Запустите позже.");
            strategyStop();
        } else if (CODE_INTERNAL_ERROR.equals(tinkoffEx.getCode())) {
            eventLogger.logError(tinkoffEx.getMessage());
        }
    }

    private void strategyStop() {
        strategyManager.stop()
                .subscribe()
                .with(unused -> log.info(">>> Stop strategy"));
    }
}
