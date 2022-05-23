package com.byby.trobot.service;

import com.byby.trobot.controller.dto.ExchangeDto;

import java.util.List;

/**
 * Сервис получения информации о биржах.
 */
public interface ExchangeService {

    /**
     * Информация о биржах в данный момент.
     * Открыты/закрыты, когда откроются.
     * Список бирж с которыми работаем берется из настроек.
     *
     * @return список информации о биржах в данный момент
     */
    List<ExchangeDto> getExchangesInfo();

    /**
     * Открытые в данный момент биржи.
     *
     * @return список открытых бирх
     */
    List<String> getExchangesOpenNow();
}
