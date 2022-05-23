package com.byby.trobot.service;

import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.Share;

import java.util.List;

/**
 * Информация об акциях.
 */
public interface SharesService {

    /**
     * Получить список акций с бирж.
     *
     * @param exhanges биржи
     * @return
     */
    Uni<List<Share>> getShares(List<String> exhanges);

    /**
     * Найти акцию по figi
     *
     * @param figi
     * @return
     */
    Uni<Share> findShareByFigi(String figi);

    /**
     * Найти тикер по figi.
     *
     * @param figi
     * @return
     */
    Uni<String> findTickerByFigi(String figi);

    /**
     * Найти тикер по figi. синхронный метод
     *
     * @param figi
     * @return
     */
    String findTickerByFigiSync(String figi);

    /**
     * Найти название по figi.
     *
     * @param figi
     * @return
     */
    String findNameByFigiSync(String figi);

    /**
     * Найти figi по тикеру.
     *
     * @param tickers
     * @return
     */
    Uni<List<String>> findFigiByTicker(List<String> tickers);

    /**
     * Список всех торгуемых акций.
     *
     * @return
     */
    Uni<List<Share>> getShares();
}
