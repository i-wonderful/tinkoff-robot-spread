package com.byby.trobot.dto.mapper;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.dto.SettingsRobotDto;
import com.google.common.base.Strings;
import java.util.stream.Collectors;

public class SettingsMapper {

    public static SettingsRobotDto toDto(ApplicationProperties properties) {

        SettingsRobotDto dto = new SettingsRobotDto();
        dto.setSandboxMode(properties.isSandboxMode());
        dto.setExchangeNames(properties.getRobotExchangeNames().stream().collect(Collectors.joining(",")));
        dto.setStrategySpreadPercent(properties.getRobotSpreadPercent());
        String tickers = properties.getFindBuyTickers()
                .stream()
                .collect(Collectors.joining(","));
        dto.setCheckBuyTickers(tickers.isEmpty() ? "Искать среди всех акций": tickers);
        dto.setTokenReal(Strings.isNullOrEmpty(properties.getTokenReal()) ? "не указан" : "***");
        dto.setTokenSandbox(Strings.isNullOrEmpty(properties.getTokenSandbox()) ? "не указан" : "***");
        return dto;
    }
}
