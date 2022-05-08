package com.byby.trobot.service.impl;

import com.byby.trobot.service.StrategyManager;
import com.byby.trobot.strategy.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class StrategyManagerImpl implements StrategyManager {
    private static final Logger log = LoggerFactory.getLogger(StrategyManagerImpl.class);

    @Inject
    Strategy strategy;

    @Override
    public void go() {
        List<String> figi = strategy.findFigi();
        log.info(">>> Find figi " + figi);
        strategy.go(figi);
    }

    @Override
    public void sellAll() {

    }
}
