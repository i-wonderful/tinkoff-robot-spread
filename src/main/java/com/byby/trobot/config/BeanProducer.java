package com.byby.trobot.config;

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
    ApplicationProperties properties;

    @Inject
    AppProperties appProperties;

    @DefaultBean
    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "false")
    public InvestApi investApi() {
        return InvestApi.create(properties.getTokenReal(), appProperties.getAppname());
    }

    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "true")
    public InvestApi investApiSandbox() {
        return InvestApi.createSandbox(properties.getTokenSandbox(), appProperties.getAppname());
    }

}
