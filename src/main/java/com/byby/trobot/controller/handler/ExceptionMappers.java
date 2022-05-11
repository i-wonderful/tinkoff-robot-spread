package com.byby.trobot.controller.handler;

import com.byby.trobot.common.GlobalBusAddress;
import io.grpc.StatusRuntimeException;
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

    @Inject
    EventBus bus;

    @ServerExceptionMapper
    public RestResponse<String> mapException(ApiRuntimeException exception) {
        log.error(">>> ", exception);
        bus.publish(GlobalBusAddress.LOG.name(), "Exception " + exception.getMessage());

        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR,
                        "Tinkoff exception ApiRuntimeException: " + exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> mapException(StatusRuntimeException exception) {
        log.error(">>> ", exception);
        bus.publish(GlobalBusAddress.LOG.name(), "Exception " + exception.getMessage());

        return RestResponse
                .status(Response.Status.INTERNAL_SERVER_ERROR,
                        "Tinkoff exception StatusRuntimeException: " + exception.getMessage());
    }
}
