package com.byby.trobot.controller.exception;

/**
 * Ошибка бизнес логики.
 * Некоторые функции приложения не доступны.
 */
public class BisinessException extends RuntimeException{
    public BisinessException(String message) {
        super(message);
    }
}
