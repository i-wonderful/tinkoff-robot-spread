package com.byby.trobot.service.impl;

import io.quarkus.vertx.ConsumeEvent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestService {

    @ConsumeEvent("greetings")
    public String hello(String name) {
        return "Hello " + name;
    }

}
