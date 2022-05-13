package com.byby.trobot.service.impl;

import com.byby.trobot.strategy.impl.model.Spread;
import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

/**
 * Сервис вычисления спредов
 */
@ApplicationScoped
public class SpreadService {
    private static final Logger log = LoggerFactory.getLogger(SpreadService.class);

    @Inject
    InvestApi api;

    @Inject
    SharesService sharesService;

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

//        log.info(">>> current prices: bid=" + bid.getPrice() + ", ask="+ask.getPrice());
        BigDecimal nextBidPrice = quotationToBigDecimal(calcNextBidPrice(figi, bid));
        BigDecimal nextAskPrice = quotationToBigDecimal(calcNextAskPrice(figi, ask));
//        log.info(">>> Calc optimal prices: bid=" + nextBidPrice + ", ask=" + nextAskPrice);
        BigDecimal diff = nextAskPrice.subtract(nextBidPrice);
        double percent = calcSpreadPercent(diff, askPrice);

        Spread spread = new Spread();
        spread.setFigi(figi);
        spread.setDiff(diff);
        spread.setPercent(percent);
        spread.setNextBidPrice(nextBidPrice);
        spread.setNextAskPrice(nextAskPrice);
        spread.setTicker(sharesService.findTickerByFigi(figi));

        return spread;
    }

    /**
     * Посчитать процент который составляем diff по отношению к price
     * @param diff
     * @param price
     * @return
     */
    private double calcSpreadPercent(BigDecimal diff, BigDecimal price) {
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return diff
                .multiply(BigDecimal.valueOf(100.0))
                .divide(price, 9, RoundingMode.CEILING)
                .doubleValue();
    }

    public Quotation calcNextBidPrice(GetOrderBookResponse orderBookResponse) {
        String figi = orderBookResponse.getFigi();
        Order bid = orderBookResponse.getBids(0);
        return calcNextBidPrice(figi, bid);
    }

    public Quotation calcNextBidPrice(String figi, Order bid) {
        return calcNextBidPrice(figi, bid.getPrice());
    }

    /**
     * Посчитать оптимальную цену для покупки по лимитной заявке.
     * Цена на одни шаг выше последней заявки не покупку из стакана.
     *
     * @param figi
     * @param bidPrice цена покупки из стакана
     * @return
     */
    public Quotation calcNextBidPrice(String figi, Quotation bidPrice) {
        //        log.info(">>> Bid price: " + MapperUtils.quotationToBigDecimal(bid.getPrice()));
        var minPriceIncrement = minPriceIncrement(figi);
        var price = Quotation.newBuilder()
                .setUnits(bidPrice.getUnits() + minPriceIncrement.getUnits())
                .setNano(bidPrice.getNano() + minPriceIncrement.getNano())
                .build();
        return price;
    }


    /**
     * Посчитать оптимальныую цену для продажи.
     * Цена на один шаг ниже последней цены в стакане.
     *
     * @param figi
     * @param ask  цена продажи из стакана
     * @return
     */
    public Quotation calcNextAskPrice(String figi, Order ask) {
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
        return api.getInstrumentsService()
                .getInstrumentByFigiSync(figi)
                .getMinPriceIncrement();
    }
}
