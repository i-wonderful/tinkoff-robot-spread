package com.byby.trobot.strategy.impl;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.service.OrderbookService;
import com.byby.trobot.service.SharesService;
import com.byby.trobot.strategy.Strategy;
import ru.tinkoff.piapi.contract.v1.Share;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SpreadStrategy implements Strategy {

    @Inject
    ApplicationProperties properties;

    @Inject
    SharesService sharesService;

    @Inject
    OrderbookService orderbookService;

    /**
     * Ищем кандидатов на покупку.
     * Если указан параметр robot.strategy.check.buy.tickers, то берем их.
     * Иначе ищем максимальные спреды среди всех.
     *
     * @return
     */
    @Override
    public List<String> findFigi() {
        List<String> tickers = properties.getCheckBuyTickers();
        if (tickers != null && !tickers.isEmpty()) {
            return sharesService.findByTicker(tickers).stream().map(Share::getFigi)
                    .collect(Collectors.toList());

        } else {
            List<Share> shares = sharesService.getShares(properties.getRobotExchangeNames());
            return orderbookService.getSpreadShapes(shares)
                    .stream()
                    .filter(s -> properties.getRobotSpreadPercent() <= s.getPercent())
                    .map(Spread::getFigi)
                    .collect(Collectors.toList());

        }
    }

    @Override
    public void go(List<String> figi) {


    }
}
