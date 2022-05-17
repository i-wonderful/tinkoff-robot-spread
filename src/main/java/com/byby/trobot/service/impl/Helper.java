package com.byby.trobot.service.impl;

import ru.tinkoff.piapi.contract.v1.Quotation;

/**
 * Вычислительные функции.
 */
public class Helper {

    /**
     * Цена плюс инкремент.
     *
     * @param bidPrice
     * @param minPriceIncrement
     * @return
     */
    public static Quotation priceBidAddIncrement(Quotation bidPrice, Quotation minPriceIncrement) {
        return Quotation.newBuilder()
                .setUnits(bidPrice.getUnits() + minPriceIncrement.getUnits())
                .setNano(bidPrice.getNano() + minPriceIncrement.getNano())
                .build();
    }

    /**
     * Цена минус инкремент.
     *
     * @param askPrice
     * @param minPriceIncrement
     * @return
     */
    public static Quotation priceAskMinusIncrement(Quotation askPrice, Quotation minPriceIncrement) {
        return Quotation.newBuilder()
                .setUnits(askPrice.getUnits() - minPriceIncrement.getUnits())
                .setNano(askPrice.getNano() - minPriceIncrement.getNano())
                .build();
    }
}
