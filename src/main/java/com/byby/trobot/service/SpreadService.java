package com.byby.trobot.service;

import com.byby.trobot.strategy.impl.model.Spread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.*;

import java.util.List;

/**
 * Сервис вычисления спредов и цен.
 */
public interface SpreadService {

    /**
     * Посчитать спред по стакану.
     *
     * @param orderBook стакан
     * @return
     */
    Uni<Spread> calcSpread(OrderBook orderBook);

    /**
     * Посчитать спред по стакану.
     *
     * @param orderBook стакан
     * @return
     */
    Uni<Spread> calcSpread(GetOrderBookResponse orderBook);

    /**
     * Посчитать спред акции.
     *
     * @param figi
     * @return
     */
    Uni<Spread> calcSpread(String figi);

    /**
     * Посчитать спред списка акций.
     *
     * @param shares
     * @return
     */
    Multi<Spread> calcSpreads(List<Share> shares);

    /**
     * Посчитать оптимальную цену для покупки.
     * Цена на один шаг ниже цены из стакана.
     *
     * @param orderBookResponse стакан
     * @return
     */
    Uni<Quotation> calcNextBidPrice(GetOrderBookResponse orderBookResponse);

    /**
     * Посчитать оптимальную цену для покупки.
     * Цена на один шаг ниже цены из стакана.
     *
     * @param figi акция
     * @param bid  заявка из стакана
     * @return
     */
    Uni<Quotation> calcNextBidPrice(String figi, Order bid);

    /**
     * Посчитать оптимальную цену для покупки.
     * Цена на один шаг ниже цены из стакана.
     *
     * @param figi     акция
     * @param bidPrice цена из стакана
     * @return
     */
    Uni<Quotation> calcNextBidPrice(String figi, Quotation bidPrice);

    /**
     * Посчитать оптимальную цену для <b>продажи</b> по лимитной заявке.
     * Цена на один шаг выше последней заявки на продажу из стакана.
     *
     * @param figi     идентификатор акции
     * @param askPrice верхняя цена продажи из стакана
     * @return
     */
    Uni<Quotation> calcNextAskPrice(String figi, Quotation askPrice);
}
