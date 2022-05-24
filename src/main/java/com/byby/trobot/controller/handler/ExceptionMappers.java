package com.byby.trobot.controller.handler;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.controller.exception.BusinessException;
import io.grpc.StatusRuntimeException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;

@ApplicationScoped
public class ExceptionMappers {
    private static final Logger log = LoggerFactory.getLogger(ExceptionMappers.class);

    @Inject
    EventLogger eventLogger;

    @Inject
    ExceptionHandler exceptionHandler;

    /**
     * Критичные ошибки мешающие всей работе.
     */
    @ServerExceptionMapper
    public RestResponse<String> map(CriticalException e) {
        log.error(">>> UserDataCriticalException", e);
        exceptionHandler.handle(e);
        return createResponse500("UserDataCriticalException: " + e.getMessage());
    }

    /**
     * Не критичные ошибки.
     */
    @ServerExceptionMapper
    public RestResponse<String> map(BusinessException e) {
        eventLogger.logError(e.getMessage());
        return createResponse400(e.getMessage());
    }

    /**
     * Ошибки при вызовах Tinkoff Invest Api
     */
    @ServerExceptionMapper
    public RestResponse<String> map(ApiRuntimeException e) {
        System.out.println(">>>>>>>>>>>>>>!!!!!!!!!!!!!!!!!!!!!! Tinkoff ApiRuntimeException: ");
        exceptionHandler.handleTinkoffException(e);
        return createResponse500("Tinkoff ApiRuntimeException: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> map(StatusRuntimeException e) {
        eventLogger.logError(e);
        return createResponse500("Tinkoff StatusRuntimeException: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> map(IllegalStateException e) {
        eventLogger.logError(e);
        return createResponse500("IllegalStateException: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> map(RuntimeException e) {
        log.info(">>> Map RuntimeException " + e);
        if(e.getCause() instanceof UnknownHostException) {
            exceptionHandler.handleCritical(e, "Проверьте соединение с интернетом!");
        }
        return createResponse500(e.getMessage());
    }

    private RestResponse<String> createResponse500(String message) {
        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    private RestResponse<String> createResponse400(String message) {
        return RestResponse
                .status(Response.Status.BAD_REQUEST, message);
    }
}
