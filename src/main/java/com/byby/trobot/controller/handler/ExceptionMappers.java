package com.byby.trobot.controller.handler;

import com.byby.trobot.common.EventLogger;
import com.byby.trobot.common.GlobalBusAddress;
import io.grpc.StatusRuntimeException;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class ExceptionMappers {
    private static final Logger log = LoggerFactory.getLogger(ExceptionMappers.class);

//    @Inject
//    EventBus bus;

    @Inject
    EventLogger eventLogger;

    @ServerExceptionMapper
    public RestResponse<String> mapException(ApiRuntimeException exception) {
        eventLogger.logError(exception);
        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR,
                        "Tinkoff exception ApiRuntimeException: " + exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(StatusRuntimeException e) {
        eventLogger.logError(e);
        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR,
                        "Tinkoff exception StatusRuntimeException: " + e.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(IllegalStateException e) {
        eventLogger.logError(e);
        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR,
                        "IllegalStateException: " + e.getMessage());
    }
}
