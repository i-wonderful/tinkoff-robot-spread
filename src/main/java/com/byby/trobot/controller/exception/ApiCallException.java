package com.byby.trobot.controller.exception;

public class ApiCallException extends RuntimeException {

    private String method;

    public ApiCallException(String message, Throwable cause, String method) {
        super(message, cause);
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

}
