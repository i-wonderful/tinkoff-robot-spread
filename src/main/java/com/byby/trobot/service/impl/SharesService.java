package com.byby.trobot.service.impl;

import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Операции с акциями
 */
@Startup
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
    public Uni<List<Share>> getShares(List<String> exhanges) {
        return getShares()
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(share -> exhanges.contains(share.getExchange()))
                        .collect(Collectors.toList()));
    }


    @Deprecated
    @CacheResult(cacheName = "shares-by-exchage-cache-sync")
    public List<Share> getSharesSync(List<String> exhanges) {
        return getSharesSync()
                .stream()
                .filter(share -> exhanges.contains(share.getExchange()))
                .collect(Collectors.toList());
    }

    @Deprecated
    @CacheResult(cacheName = "ticker-by-figi-cache-sync")
    public String findTickerByFigiSync(String figi) {
        return findTickerByFigi(figi).await().indefinitely();
    }

    @CacheResult(cacheName = "ticker-by-figi-cache")
    public Uni<String> findTickerByFigi(String figi) {
        return getShares()
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(sh -> figi.equals(sh.getFigi()))
                        .findFirst()
                        .map(Share::getTicker)
                        .orElse(null));
    }

    @CacheResult(cacheName = "name-by-figi-cache")
    public String findNameByFigi(String figi) {
        log.info(">>> findNameByFigi");
        return getSharesSync().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getName)
                .orElse(null);
    }

    @CacheResult(cacheName = "shares-by-ticker-cache")
    public List<Share> findByTicker(List<String> tickers) {
        return getSharesSync()
                .stream()
                .filter(share -> tickers.contains(share.getTicker()))
                .collect(Collectors.toList());
    }

    @CacheResult(cacheName = "shares-cache")
    public Uni<List<Share>> getShares() {
        log.info(">>> Get All Shares Async");
        return Uni.createFrom().completionStage(instrumentsService.getTradableShares())
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                        .collect(Collectors.toList()));
    }

    @Deprecated
    @CacheResult(cacheName = "shares-cache-sync")
    protected List<Share> getSharesSync() {
        System.out.println(">>> GetShares Sync");
        return instrumentsService.getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .collect(Collectors.toList());
    }
}
