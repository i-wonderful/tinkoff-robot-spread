package com.byby.trobot.service.impl;

import com.byby.trobot.strategy.impl.Spread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderBook;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    public static Spread calcSpread(OrderBook orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi, BigDecimal.ZERO, 0.0);
        }

        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);

        return calcSpread(ask, bid, figi);
    }

    public static Spread calcSpread(GetOrderBookResponse orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi, BigDecimal.ZERO, 0.0);
        }

        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);

        return calcSpread(ask, bid, figi);
    }

    public static double calcSpreadPercent(BigDecimal askPrice, BigDecimal bidPrice) {
        BigDecimal diff = askPrice.remainder(bidPrice);
        return diff.multiply(BigDecimal.valueOf(100.0)).divide(askPrice, 9, RoundingMode.CEILING).doubleValue();
    }

    private static Spread calcSpread(Order ask, Order bid, String figi) {
        BigDecimal askPrice = quotationToBigDecimal(ask.getPrice());
        BigDecimal bidPrice = quotationToBigDecimal(bid.getPrice());

        BigDecimal diff = askPrice.remainder(bidPrice); // todo
        double percent = calcSpreadPercent(askPrice, bidPrice);

        return new Spread(figi, diff, percent);
    }
}
