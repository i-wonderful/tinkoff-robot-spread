package com.byby.trobot.controller.handler;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.service.SharesService;
import com.byby.trobot.strategy.StrategyManager;
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
    private final static String CODE_MARGIN_ERROR = "30042";
    private final static String CODE_ERROR_CANCEL_ORDER = "50005";

    @Inject
    EventLogger eventLogger;

    @Inject
    StrategyManager strategyManager;

    @Inject
    SharesService sharesService;

    /**
     * Обработчик критических
     *
     * @param throwable
     * @param additionalInfo
     */
    public void handleCritical(Throwable throwable, String additionalInfo) {
        log.error(">>> Critical Error: " + throwable.getMessage() + " " + additionalInfo);
        eventLogger.logCritical(additionalInfo + " " + throwable.getMessage());
        handle(throwable);
    }

    /**
     * Обработчик ошибок
     *
     * @param throwable      эксепшн tinkoffApi
     * @param additionalInfo дополнительныйй информация
     * @param figi           идентификатор акции если есть
     */
    public void handle(Throwable throwable, String additionalInfo, String figi) {
        handle(throwable);

        if (figi == null) {
            eventLogger.logError(additionalInfo);
        }

        sharesService.findTickerByFigi(figi)
                .subscribe()
                .with(ticker -> {
                    String message = String.format("[%s] %s", ticker, additionalInfo);
                    eventLogger.logError(message);
                });
    }

    public void handle(Throwable throwable, String additionalInfo) {
        handle(throwable, additionalInfo, null);
    }

    public void handle(Throwable throwable) {
        if (throwable instanceof ApiRuntimeException) {
            handleTinkoffException((ApiRuntimeException) throwable);
        } else if (throwable instanceof CriticalException) {
            handle((CriticalException) throwable);
        } else {
            log.error("Oh no! We received a failure: " + throwable.getMessage());
        }
    }

    public void handle(CriticalException e) {
        log.info(">>>> Handle CriticalException");
        eventLogger.logCritical(e.getMessage());
        eventLogger.log("Критическая ошибка. Останавливаем робота.");
        strategyStop();
        if (e.getSourceThrMessage() != null) {
            eventLogger.logCritical(e.getSourceThrMessage());
        }
    }

    public void handleTinkoffException(ApiRuntimeException tinkoffEx) {
        if (CODE_ERROR_LIMIT.equals(tinkoffEx.getCode())) {
            eventLogger.logError(tinkoffEx.getMessage());
            eventLogger.log("Ошибка. Превышены лимиты. Останавливаем робота. Запустите позже.");
            strategyStop();
        } else {
            eventLogger.logError(tinkoffEx.getMessage());
        }
//        else if (CODE_INTERNAL_ERROR.equals(tinkoffEx.getCode())) {
//            eventLogger.logError(tinkoffEx.getMessage());
//        } else if (CODE_MARGIN_ERROR.equals(tinkoffEx.getCode())) {
//            eventLogger.logError(tinkoffEx.getMessage());
//        }
    }

    private void strategyStop() {
        strategyManager.stop()
                .subscribe()
                .with(unused -> log.info(">>> Stop strategy"));
    }
}
