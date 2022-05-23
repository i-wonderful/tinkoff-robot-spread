package com.byby.trobot.config;

import com.byby.trobot.controller.exception.CriticalException;
import com.byby.trobot.controller.handler.ExceptionHandler;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.Startup;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Startup
@Dependent
public class BeanProducer {

    @Inject
    RobotProperties properties;

    @DefaultBean
    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "false")
    public InvestApi investApi() {
        if(!properties.tokenReal().isPresent()) {
            throw new CriticalException("Не указан токен для работы с реальным счетом.");
        }
        return InvestApi.create(properties.tokenReal().get(), properties.appname());
    }

    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "true")
    public InvestApi investApiSandbox() {
        if (!properties.tokenSandbox().isPresent()){
            throw new CriticalException("Не указан токен для работы со счетом песочницы.");
        }
        return InvestApi.createSandbox(properties.tokenSandbox().get(), properties.appname());
    }

}
