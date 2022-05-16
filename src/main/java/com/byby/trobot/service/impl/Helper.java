package com.byby.trobot.service.impl;

import ru.tinkoff.piapi.contract.v1.Quotation;

public class Helper {

    public static Quotation priceBidAddIncrement(Quotation bidPrice, Quotation minPriceIncrement) {
        return Quotation.newBuilder()
                .setUnits(bidPrice.getUnits() + minPriceIncrement.getUnits())
                .setNano(bidPrice.getNano() + minPriceIncrement.getNano())
                .build();
    }

    public static Quotation priceAskMinusIncrement(Quotation askPrice, Quotation minPriceIncrement) {
        return Quotation.newBuilder()
                .setUnits(askPrice.getUnits() - minPriceIncrement.getUnits())
                .setNano(askPrice.getNano() - minPriceIncrement.getNano())
                .build();
    }
}
