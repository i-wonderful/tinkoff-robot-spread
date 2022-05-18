package com.byby.trobot.service.impl;

import com.byby.trobot.strategy.impl.model.Spread;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.MarketDataService;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

/**
 * Сервис вычисления спредов
 */
@ApplicationScoped
public class SpreadService {
    private static final Logger log = LoggerFactory.getLogger(SpreadService.class);

    private final MarketDataService marketDataService;
    private final InstrumentsService instrumentsService;
    private final SharesService sharesService;

    public SpreadService(InvestApi api, SharesService sharesService) {
        this.instrumentsService = api.getInstrumentsService();
        this.marketDataService = api.getMarketDataService();
        this.sharesService = sharesService;
    }

    public Uni<Spread> calcSpread(OrderBook orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            return Uni.createFrom().item(new Spread(figi));
        }
        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);
        return calcSpread(ask, bid, figi);
    }

    public Uni<Spread> calcSpread(GetOrderBookResponse orderBook) {
        String figi = orderBook.getFigi();
        if (orderBook.getAsksCount() < 1 || orderBook.getBidsCount() < 1) {
            return Uni.createFrom().item(new Spread(figi));
        }
        Order ask = orderBook.getAsks(0);
        Order bid = orderBook.getBids(0);
        return calcSpread(ask, bid, figi);
    }

    public Uni<Spread> getSpread(String figi) {
        return Uni.createFrom().completionStage(marketDataService.getOrderBook(figi, 1))
                .onItem()
                .transformToUni(orderBookResponse -> calcSpread(orderBookResponse));
    }

    public Spread getSpreadSync(String figi) {
        return getSpread(figi).await().indefinitely();
    }

//    public List<Spread> getSpreadsSync(List<Share> share) {
//        return share.stream()
//                .map(Share::getFigi)
//                .map(this::getSpreadSync)
//                .collect(Collectors.toList());
//    }

    public Multi<Spread> getSpreads(List<Share> share) {
        List<Uni<Spread>> spreads = share.stream()
                .map(Share::getFigi)
                .map(this::getSpread)
                .collect(Collectors.toList());
        //return Uni.combine().all().unis(spreads);

        return Multi.createFrom().iterable(spreads).onItem()
                .transformToMulti(Uni::toMulti)
                .merge();
        // return Multi.createFrom().iterable(spreads) ;
    }

//    public List<Spread> getSpread(List<String> figi) {
//        return figi.stream()
//                .map(f -> getSpreadSync(f))
//                .filter(spread -> !BigDecimal.ZERO.equals(spread.getDiff())) // todo убрать?
//                .sorted(Comparator.comparingDouble(Spread::getPercent).reversed())
//                .collect(Collectors.toList());
//    }


    private Uni<Spread> calcSpread(Order ask, Order bid, String figi) {
        BigDecimal askPrice = quotationToBigDecimal(ask.getPrice());

        /*
        (figi)
                .onItem()
                .transform(minPriceIncrement ->
                        Quotation.newBuilder()
                                .setUnits(bidPrice.getUnits() + minPriceIncrement.getUnits())
                                .setNano(bidPrice.getNano() + minPriceIncrement.getNano())
                                .build());
        * */
        Uni uni0 = minPriceIncrement(figi);
//        Uni<Quotation> uni1 = calcNextBidPrice(figi, bid);
//        Uni<Quotation> uni2 = uni1.chain(quotation -> calcNextAskPrice(figi, ask));

//        uni1.subscribe().with(quotation -> calcNextAskPrice(figi, ask));
        return Uni.combine().all()
                .unis(minPriceIncrement(figi),
                        sharesService.findShareByFigi(figi))
                .asTuple()
                .onItem()
                .transform(triple -> {
                    Quotation minPriceIncrement = triple.getItem1();
                    String ticker = triple.getItem2().getTicker();
                    String currency = triple.getItem2().getCurrency();

                    BigDecimal nextBidPrice = quotationToBigDecimal(Helper.priceBidAddIncrement(bid.getPrice(), minPriceIncrement));
                    BigDecimal nextAskPrice = quotationToBigDecimal(Helper.priceAskMinusIncrement(ask.getPrice(), minPriceIncrement));

                    BigDecimal diff = nextAskPrice.subtract(nextBidPrice);
                    double percent = calcSpreadPercent(diff, askPrice);

                    Spread spread = new Spread();
                    spread.setFigi(figi);
                    spread.setDiff(diff);
                    spread.setPercent(percent);
                    spread.setNextBidPrice(nextBidPrice);
                    spread.setNextAskPrice(nextAskPrice);
                    spread.setTicker(ticker);
                    spread.setCurrency(currency);
                    return spread;
                });
    }

    /**
     * Посчитать процент который составляем diff по отношению к price
     *
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

    public Uni<Quotation> calcNextBidPrice(GetOrderBookResponse orderBookResponse) {
        if (orderBookResponse.getBidsCount() < 1) {
            log.info(">>> Bids count is 0");
            return Uni.createFrom().item(Quotation.newBuilder().build());
        }
        String figi = orderBookResponse.getFigi();
        Order bid = orderBookResponse.getBids(0);
        return calcNextBidPrice(figi, bid);
    }

    /**
     * Посчитать оптимальную цену для покупки по лимитной заявке.
     * Цена на одни шаг выше последней заявки на покупку из стакана.
     *
     * @param figi
     * @param bid  заявка покупки из стакана
     * @return
     */
    public Uni<Quotation> calcNextBidPrice(String figi, Order bid) {
        return calcNextBidPrice(figi, bid.getPrice());
    }

    public Uni<Quotation> calcNextBidPrice(String figi, Quotation bidPrice) {
        return minPriceIncrement(figi)
                .onItem()
                .transform(minPriceIncrement -> Helper.priceBidAddIncrement(bidPrice, minPriceIncrement));
    }


    /**
     * Посчитать оптимальную цену для <b>продажи</b> по лимитной заявке.
     * Цена на один шаг выше последней заявки на продажу из стакана.
     *
     * @param figi идентификатор акции
     * @param ask  верхняя заявка продажи из стакана
     * @return
     */
    public Uni<Quotation> calcNextAskPrice(String figi, Order ask) {
        return calcNextAskPrice(figi, ask.getPrice());
    }

    public Uni<Quotation> calcNextAskPrice(String figi, Quotation askPrice) {
        return minPriceIncrement(figi)
                .onItem()
                .transform(minPriceIncrement -> Helper.priceAskMinusIncrement(askPrice, minPriceIncrement));
    }

//    @Deprecated
//    @CacheResult(cacheName = "min-price-increment-sync")
//    protected Quotation minPriceIncrementSync(String figi) {
//        return minPriceIncrement(figi).await().indefinitely();
//    }

    @CacheResult(cacheName = "min-price-increment")
    protected Uni<Quotation> minPriceIncrement(String figi) {
//        log.info(">>> minPriceIncrement " + figi);
        return Uni.createFrom()
                .completionStage(instrumentsService.getInstrumentByFigi(figi))
                .onItem()
                .transform(instrument -> instrument.getMinPriceIncrement());
    }
}
