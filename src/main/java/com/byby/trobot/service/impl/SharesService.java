package com.byby.trobot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Операции с акциями
 */
@ApplicationScoped
public class SharesService {
    private static final Logger log = LoggerFactory.getLogger(SharesService.class);

    @Inject
    InvestApi api;

    /**
     * @return
     */
    public Quotation calcMinBuyPrice(String figi) {
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
     *
     * @param exhanges
     * @return
     */
    public List<Share> getShares(List<String> exhanges) {
        List<Share> shares = getShares().stream()
                .filter(share -> exhanges.contains(share.getExchange()))
                .collect(Collectors.toList());

        return shares;
    }

    private List<Share> getShares() {
        return api.getInstrumentsService().getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .collect(Collectors.toList());
    }

    public String findTickerByFigi(String figi) {
        return getShares().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getTicker)
                .orElse(null);
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
