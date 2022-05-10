package com.byby.trobot.config;

import com.byby.trobot.executor.Executor;
import com.byby.trobot.executor.impl.RealExecutor;
import com.byby.trobot.executor.impl.SandboxExecutor;
import com.byby.trobot.service.impl.SharesService;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.Startup;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Startup
@Dependent
public class ApplicationConfig {

    @Inject
    ApplicationProperties properties;

    @DefaultBean
    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "false")
    public InvestApi investApi() {
        System.out.println(">>> Init real executor");
        return InvestApi.create(properties.getTokenReal());
    }

//    @DefaultBean
//    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "false")
//    public Executor executor(InvestApi api){
//        return new RealExecutor(api);
//    }

    @ApplicationScoped
    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "true")
    public InvestApi investApiSandbox() {
        return InvestApi.createSandbox(properties.getTokenSandbox());
    }

//    @ApplicationScoped
//    @IfBuildProperty(name = "robot.sandbox.mode", stringValue = "true")
//    public Executor executorSandbox(InvestApi api, SharesService sharesService){
//        return new SandboxExecutor(api, sharesService);
//    }


}