package com.byby.trobot.controller.dto.mapper;

import com.byby.trobot.config.RobotProperties;
import com.byby.trobot.config.RobotSandboxProperties;
import com.byby.trobot.config.StrategySharesProperties;
import com.byby.trobot.controller.dto.SettingsRobotDto;

import java.util.Collections;
import java.util.stream.Collectors;

public class SettingsMapper {

    public static SettingsRobotDto toDto(RobotSandboxProperties sandboxProperties,
                                         StrategySharesProperties strategySharesProperties,
                                         RobotProperties robotProperties) {

        SettingsRobotDto dto = new SettingsRobotDto();
        dto.setSandboxMode(sandboxProperties.isSandboxMode());
        dto.setExchangeNames(robotProperties.exchangeNames().stream().collect(Collectors.joining(",")));
        dto.setStrategySpreadPercent(strategySharesProperties.spreadPercent());
        String tickers = strategySharesProperties.tickersFind().orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.joining(","));
        dto.setCheckBuyTickers(tickers.isEmpty() ? "Искать среди всех акций" : tickers);
        dto.setTokenReal(robotProperties.tokenReal().isPresent() ? "***" : "не указан");
        dto.setTokenSandbox(robotProperties.tokenSandbox().isPresent() ? "***" : "не указан");
        return dto;
    }
}
