package com.byby.trobot.service.impl;

import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.utils.MapperUtils;

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
    // todo проверить
    @Deprecated
    public Quotation calcMinBuyPrice(String figi) {
        var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        log.info(">>> lastPrice: " + MapperUtils.quotationToBigDecimal(lastPrice));
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        log.info(">>> Min price increment: " + MapperUtils.quotationToBigDecimal(minPriceIncrement));
        var price = Quotation.newBuilder()
                .setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano())
                .build();
        return price;
    }



    /**
     * Получить список акций с бирж
     *
     * @param exhanges
     * @return
     */
    @CacheResult(cacheName = "shares-by-exchage-cache")
    public List<Share> getShares(List<String> exhanges) {
        return getShares()
                .stream()
                .filter(share -> exhanges.contains(share.getExchange()))
                .collect(Collectors.toList());
    }

    @CacheResult(cacheName = "shares-cache")
    protected List<Share> getShares() {
        return api.getInstrumentsService().getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .collect(Collectors.toList());
    }

    @CacheResult(cacheName = "ticker-by-figi-cache")
    public String findTickerByFigi(String figi) {
        return getShares().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getTicker)
                .orElse(null);
    }

    @CacheResult(cacheName = "shares-by-ticker-cache")
    public List<Share> findByTicker(List<String> tickers) {
        return getShares()
                .stream()
                .filter(share -> tickers.contains(share.getTicker()))
                .collect(Collectors.toList());
    }
}
