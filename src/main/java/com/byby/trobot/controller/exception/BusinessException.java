package com.byby.trobot.controller.exception;

/**
 * Ошибка бизнес логики.
 * Некоторые функции приложения не доступны.
 */
public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }
}
