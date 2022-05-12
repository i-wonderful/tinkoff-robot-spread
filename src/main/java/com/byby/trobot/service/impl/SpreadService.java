package com.byby.trobot.service.impl;

import com.byby.trobot.strategy.impl.model.Spread;
import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

@ApplicationScoped
public class SpreadService {
    private static final Logger log = LoggerFactory.getLogger(SpreadService.class);

    @Inject
    InvestApi api;

    public Spread calcSpread(OrderBook orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi, BigDecimal.ZERO, 0.0);
        }
        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);
        return calcSpread(ask, bid, figi);
    }

    public Spread calcSpread(GetOrderBookResponse orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            log.info(">>> Empty Order book, figi {} ", figi);
            return new Spread(figi);
        }
        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);
        return calcSpread(ask, bid, figi);
    }

    public Spread getSpread(String figi) {
        var orderBook = api.getMarketDataService().getOrderBookSync(figi, 1);
        return calcSpread(orderBook);
    }

    public List<Spread> getSpreads(List<Share> share) {
        return share.stream()
                .map(Share::getFigi)
                .map(this::getSpread)
                .collect(Collectors.toList());
    }

    public List<Spread> getSpread(List<String> figi) {
        return figi.stream()
                .map(f -> getSpread(f))
                .filter(spread -> !BigDecimal.ZERO.equals(spread.getDiff())) // todo убрать?
                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
                .collect(Collectors.toList());
    }




    private Spread calcSpread(Order ask, Order bid, String figi) {
        BigDecimal askPrice = quotationToBigDecimal(ask.getPrice());

        BigDecimal minBuyPrice = quotationToBigDecimal(calcMinBuyPrice(figi, bid));
        BigDecimal maxAskPrice = quotationToBigDecimal(calcMaxAskPrice(figi, ask));
        BigDecimal diff = maxAskPrice.remainder(minBuyPrice);
        double percent = calcSpreadPercent(diff, askPrice);

        Spread spread = new Spread();
        spread.setFigi(figi);
        spread.setDiff(diff);
        spread.setPercent(percent);
        spread.setMinBuyPrice(minBuyPrice);
        spread.setMaxAskPrice(maxAskPrice);

        return spread;
    }

    private double calcSpreadPercent(BigDecimal diff, BigDecimal askPrice) {
        return diff
                .multiply(BigDecimal.valueOf(100.0))
                .divide(askPrice, 9, RoundingMode.CEILING)
                .doubleValue();
    }

    public Quotation calcMinBuyPrice(GetOrderBookResponse orderBookResponse) {
        String figi = orderBookResponse.getFigi();
        Order bid = orderBookResponse.getBids(0);
        return calcMinBuyPrice(figi, bid);
    }

    public Quotation calcMinBuyPrice(String figi, Order bid) {
//        log.info(">>> Bid price: " + MapperUtils.quotationToBigDecimal(bid.getPrice()));
        var minPriceIncrement = minPriceIncrement(figi);
//        log.info(">>> price increment: " + MapperUtils.quotationToBigDecimal(minPriceIncrement));
        var price = Quotation.newBuilder()
                .setUnits(bid.getPrice().getUnits() + minPriceIncrement.getUnits())
                .setNano(bid.getPrice().getNano() + minPriceIncrement.getNano())
                .build();
        return price;
    }

    public Quotation calcMaxAskPrice(String figi, Order ask) {
//        log.info(">>> Ask price: " + MapperUtils.quotationToBigDecimal(ask.getPrice()));
        var minPriceIncrement = minPriceIncrement(figi);
//        log.info(">>> price increment: " + MapperUtils.quotationToBigDecimal(minPriceIncrement));
        var price = Quotation.newBuilder()
                .setUnits(ask.getPrice().getUnits() - minPriceIncrement.getUnits())
                .setNano(ask.getPrice().getNano() - minPriceIncrement.getNano())
                .build();
        return price;
    }

    @CacheResult(cacheName = "min-price-increment")
    protected Quotation minPriceIncrement(String figi) {
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        return minPriceIncrement;
    }
}
