package com.byby.trobot.controller.exception;

import java.util.Optional;

/**
 * Критическая ошибка.
 * Работа приложения невозможна.
 */
public class CriticalException extends RuntimeException {
    private Throwable sourceThr;

    public CriticalException(Throwable thr, String message) {
        super(message);
        this.sourceThr = thr;
    }

    public CriticalException(String message) {
        super(message);
    }

    public Throwable getSourceThr() {
        return sourceThr;
    }

    public String getSourceThrMessage(){
        return Optional.ofNullable(sourceThr).map(Throwable::getMessage)
                .orElse(null);
    }
}
