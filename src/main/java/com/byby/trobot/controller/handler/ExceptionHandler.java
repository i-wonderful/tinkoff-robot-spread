package com.byby.trobot.controller.handler;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.service.StrategyManager;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ExceptionHandler {
    private final static String CODE_ERROR_LIMIT =  "80002";
    @Inject
    EventLogger eventLogger;

    @Inject
    StrategyManager strategyManager;

    public void handle(Throwable throwable) {
        if (throwable instanceof ApiRuntimeException) {
            ApiRuntimeException tinkoffEx = (ApiRuntimeException) throwable;
            if(CODE_ERROR_LIMIT.equals(tinkoffEx.getCode())) {
                eventLogger.log(tinkoffEx.getMessage());
                eventLogger.log("Превышены лимиты. Останавливаем робота. Запустите позже.");
                strategyManager.stop()
                        .subscribe()
                        .with(unused -> System.out.println(">>> Stop strategy"));
            }
            System.out.println("Oh no! We received a failure: " + tinkoffEx.getMessage());
            eventLogger.logError(tinkoffEx);
        }
    }
}
