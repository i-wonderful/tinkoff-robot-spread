package com.byby.trobot.strategy;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface FindFigiService {
    /**
     * Найти акции-кандидаты для покупки
     *
     * @return
     */
    Uni<List<String>> findFigi();

    /**
     *
     */
    void stopTimers();
}
