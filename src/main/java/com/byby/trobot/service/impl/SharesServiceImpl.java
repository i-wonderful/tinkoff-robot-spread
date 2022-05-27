package com.byby.trobot.service.impl;

import com.byby.trobot.service.SharesService;
import io.quarkus.cache.CacheResult;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Share;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.InvestApi;

import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Информация об акциях.
 *
 * Будем рассуждать логично, что количество акций на биржах, по крайней мере в течении одного сеанса, не меняется,
 * поэтому кешируем запросы, дабы снизить нагрузку.
 */
@Singleton
@Startup
public class SharesServiceImpl implements SharesService {
    private static final Logger log = LoggerFactory.getLogger(SharesServiceImpl.class);

    private InstrumentsService instrumentsService;

    public SharesServiceImpl(InvestApi api) {
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
    @CacheResult(cacheName = "ticker-by-figi-cache-sync")
    public String findTickerByFigiSync(String figi) {
        return findTickerByFigi(figi).await().indefinitely();
    }

    @CacheResult(cacheName = "share-by-figi-cache")
    public Uni<Share> findShareByFigi(String figi) {
        return getShares()
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(sh -> figi.equals(sh.getFigi()))
                        .findFirst()
                        .orElse(null));
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

    @Deprecated
    @CacheResult(cacheName = "name-by-figi-cache")
    public String findNameByFigiSync(String figi) {
        return getSharesSync().stream()
                .filter(sh -> figi.equals(sh.getFigi()))
                .findFirst()
                .map(Share::getName)
                .orElse(null);
    }

    /**
     * Найти список figi акций по их тикерам.
     *
     * @param tickers
     * @return
     */
    @CacheResult(cacheName = "figi-by-ticker-cache")
    public Uni<List<String>> findFigiByTicker(List<String> tickers) {
        if (tickers.isEmpty()) {
            return Uni.createFrom().nothing();
        }
        return getShares()
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(share -> tickers.contains(share.getTicker()))
                        .map(Share::getFigi)
                        .collect(Collectors.toList())
               );
    }

    @Deprecated
    @CacheResult(cacheName = "shares-by-ticker-cache-sync")
    public List<Share> findByTickerSync(List<String> tickers) {
        return getSharesSync()
                .stream()
                .filter(share -> tickers.contains(share.getTicker()))
                .collect(Collectors.toList());
    }

    @CacheResult(cacheName = "shares-cache")
    public Uni<List<Share>> getShares() {
        log.info(">>> API Call: instrumentsService.getTradableShares()");
        return Uni.createFrom().completionStage(instrumentsService.getTradableShares())
                .onItem()
                .transform(shares -> shares.stream()
                        .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                        .collect(Collectors.toList()));
    }

    @Deprecated
    @CacheResult(cacheName = "shares-cache-sync")
    protected List<Share> getSharesSync() {
        log.info(">>> API Call: instrumentsService.getTradableSharesSync()");
        return instrumentsService.getTradableSharesSync()
                .stream()
                .filter(share -> Boolean.TRUE.equals(share.getApiTradeAvailableFlag()))
                .collect(Collectors.toList());
    }
}
