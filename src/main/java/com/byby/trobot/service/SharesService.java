package com.byby.trobot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SharesService {
    private static final Logger log = LoggerFactory.getLogger(SharesService.class);

    @Inject
    InvestApi api;

    /**
     *
     * @return
     */
    public Quotation calcMinBuyPrice(String figi){
        var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        log.info(">>> Last Price: " + lastPrice);
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        log.info(">>> Min PriceIncrement: " + minPriceIncrement);
        var price = Quotation.newBuilder()
                .setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano())
                .build();
        log.info(">>> Price: " + price);

        return price;
    }

    @Deprecated
    public List<String> randomFigi(int count) {
        List<Share> shares = api.getInstrumentsService().getTradableSharesSync();
//        System.out.println(">> Total Shares: " + shares.size());
        return shares
                .stream()
                .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
                .map(Share::getFigi)
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Получить список акций с бирж
     * @param exhanges
     * @return
     */
    public List<Share> getShares(List<String> exhanges) {
        return api.getInstrumentsService().getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .filter(share -> exhanges.contains(share.getExchange()))
                .collect(Collectors.toList());
    }



    public void printByFigi(InvestApi api){
        var instrument = api.getInstrumentsService().getInstrumentByFigiSync("BBG000B9XRY4");
        log.info(
                "инструмент figi: {}, лотность: {}, текущий режим торгов: {}, признак внебиржи: {}, признак доступности торгов " +
                        "через api : {}",
                instrument.getFigi(),
                instrument.getLot(),
                instrument.getTradingStatus().name(),
                instrument.getOtcFlag(),
                instrument.getApiTradeAvailableFlag());
    }

    public List<Share> findByTicker(List<String> tickers) {
        List<Share> shares = api.getInstrumentsService().getTradableSharesSync();
        return shares
                .stream()
                .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
                .filter(share -> tickers.contains(share.getTicker()))
                .collect(Collectors.toList());
    }
}
