package com.byby.trobot.strategy;

import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Стратегия опеределяет что и когда купить и продать.
 */
public interface Strategy {

    /**
     * Отписаться от стримов.
     *
     * @param figiUnsucscribe акции
     */
    Uni<Void> stopListening(List<String> figiUnsucscribe);

    /**
     * Запустить стратегию
     *
     * @param figi акции которыми буем торговать.
     */
    void start(List<String> figi);

}
