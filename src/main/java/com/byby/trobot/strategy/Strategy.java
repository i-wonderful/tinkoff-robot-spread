package com.byby.trobot.strategy;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface Strategy {

    /**
     *
     * @param figi
     */
    void start(List<String> figi);

    /**
     *
     */
    Uni<Void> stop();
}
