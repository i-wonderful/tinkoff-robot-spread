package com.byby.trobot.service.impl;

import io.quarkus.cache.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InstrumentsService;
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

    private InstrumentsService instrumentsService;

    public SharesService(InvestApi api) {
        instrumentsService = api.getInstrumentsService();
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

    @CacheResult(cacheName = "ticker-by-figi-cache")
    public String findTickerByFigi(String figi) {
        return getShares().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getTicker)
                .orElse(null);
    }

    @CacheResult(cacheName = "name-by-figi-cache")
    public String findNameByFigi(String figi) {
        return getShares().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getName)
                .orElse(null);
    }

    @CacheResult(cacheName = "shares-by-ticker-cache")
    public List<Share> findByTicker(List<String> tickers) {
        return getShares()
                .stream()
                .filter(share -> tickers.contains(share.getTicker()))
                .collect(Collectors.toList());
    }

    @CacheResult(cacheName = "shares-cache")
    protected List<Share> getShares() {
        System.out.println(">>> GetShares");
        return instrumentsService.getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .collect(Collectors.toList());
    }
}
