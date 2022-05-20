package com.byby.trobot.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppProperties {

    @ConfigProperty(name = "robot.margin.allow")
    boolean marginAllow;

    @ConfigProperty(name = "robot.appname")
    String appname;

    public boolean isMarginAllow() {
        return marginAllow;
    }

    public String getAppname() {
        return appname;
    }
}
