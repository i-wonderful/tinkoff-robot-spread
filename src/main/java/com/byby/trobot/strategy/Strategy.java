package com.byby.trobot.strategy;

import java.util.List;

public interface Strategy {
    /**
     * Найти акции-кандидаты для покупки
     * @return
     */
    List<String> findFigi();

    /**
     *
     * @param figi
     */
    void go(List<String> figi);
}
