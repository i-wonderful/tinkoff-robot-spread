package com.byby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

// с примера https://github.com/Tinkoff/invest-api-java-sdk/blob/5d61a6e27eb8a7bfefa871cf2c8f8e0082d1017e/example/src/main/java/ru/tinkoff/piapi/example/Example.java#L418
@Deprecated
public class Examples {
    private static Logger log = LoggerFactory.getLogger(Examples.class);

    public static void ordersStreamExample(InvestApi api) {
        StreamProcessor<TradesStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                log.info("пинг сообщение");
            } else if (response.hasOrderTrades()) {
                log.info("Новые данные по сделкам: {}", response);
            }
        };

        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        //Подписка стрим сделок. Не блокирующий вызов
        //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
        api.getOrdersStreamService().subscribeTrades(consumer, onErrorCallback);

        //Если обработка ошибок не требуется, то можно использовать перегруженный метод
        api.getOrdersStreamService().subscribeTrades(consumer);
    }

    public static void marketdataStreamExample(InvestApi api) {
        var randomFigi = randomFigi(api, 5);

        //Описываем, что делать с приходящими в стриме данными
        StreamProcessor<MarketDataResponse> processor = response -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("пинг сообщение");
            } else if (response.hasCandle()) {
                log.info("Новые данные по свечам: {}", response);
            } else if (response.hasOrderbook()) {
                log.info("Новые данные по стакану: {}", response);
            } else if (response.hasTrade()) {
                log.info("Новые данные по сделкам: {}", response);
            } else if (response.hasSubscribeCandlesResponse()) {
                var successCount = response.getSubscribeCandlesResponse().getCandlesSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на свечи: {}", successCount);
                log.info("неудачных подписок на свечи: {}", errorCount);
            } else if (response.hasSubscribeInfoResponse()) {
                var successCount = response.getSubscribeInfoResponse().getInfoSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на статусы: {}", successCount);
                log.info("неудачных подписок на статусы: {}", errorCount);
            } else if (response.hasSubscribeOrderBookResponse()) {
                var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на стакан: {}", successCount);
                log.info("неудачных подписок на стакан: {}", errorCount);
            } else if (response.hasSubscribeTradesResponse()) {
                var successCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на сделки: {}", successCount);
                log.info("неудачных подписок на сделки: {}", errorCount);
            } else if (response.hasSubscribeLastPriceResponse()) {
                var successCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на последние цены: {}", successCount);
                log.info("неудачных подписок на последние цены: {}", errorCount);
            }
        };
        Consumer<Throwable> onErrorCallback = error -> log.error(error.toString());

        //Подписка на список инструментов. Не блокирующий вызов
        //При необходимости обработки ошибок (реконнект по вине сервера или клиента), рекомендуется сделать onErrorCallback
        api.getMarketDataStreamService().newStream("trades_stream", processor, onErrorCallback).subscribeTrades(randomFigi);
        api.getMarketDataStreamService().newStream("candles_stream", processor, onErrorCallback).subscribeCandles(randomFigi);
        api.getMarketDataStreamService().newStream("info_stream", processor, onErrorCallback).subscribeInfo(randomFigi);
        api.getMarketDataStreamService().newStream("orderbook_stream", processor, onErrorCallback).subscribeOrderbook(randomFigi);
        api.getMarketDataStreamService().newStream("last_prices_stream", processor, onErrorCallback).subscribeLastPrices(randomFigi);


        //Для стримов стаканов и свечей есть перегруженные методы с дефолтными значениями
        //глубина стакана = 10, интервал свечи = 1 минута
        api.getMarketDataStreamService().getStreamById("trades_stream").subscribeOrderbook(randomFigi);
        api.getMarketDataStreamService().getStreamById("candles_stream").subscribeCandles(randomFigi);

        //Отписка на список инструментов. Не блокирующий вызов
        api.getMarketDataStreamService().getStreamById("trades_stream").unsubscribeTrades(randomFigi);
        api.getMarketDataStreamService().getStreamById("candles_stream").unsubscribeCandles(randomFigi);
        api.getMarketDataStreamService().getStreamById("info_stream").unsubscribeInfo(randomFigi);
        api.getMarketDataStreamService().getStreamById("orderbook_stream").subscribeOrderbook(randomFigi);
        api.getMarketDataStreamService().getStreamById("last_prices_stream").unsubscribeLastPrices(randomFigi);

        //Каждый marketdata стрим может отдавать информацию максимум по 300 инструментам
        //Если нужно подписаться на большее количество, есть 2 варианта:
        // - открыть новый стрим
        api.getMarketDataStreamService().newStream("new_stream", processor, onErrorCallback).subscribeCandles(randomFigi);
        // - отписаться от инструментов в существующем стриме, освободив место под новые
        api.getMarketDataStreamService().getStreamById("new_stream").unsubscribeCandles(randomFigi);
    }


    private static List<String> randomFigi(InvestApi api, int count) {
        List<Share> shares = api.getInstrumentsService().getTradableSharesSync();
//        System.out.println(">> Total Shares: " + shares.size());
        return shares
                .stream()
                .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
                .map(Share::getFigi)
                .limit(count)
                .collect(Collectors.toList());
    }

    public static void getOrderbookExample(InvestApi api) {

        //Получаем и печатаем стакан для инструмента
        var figi = randomFigi(api, 1).get(0);
        var depth = 10;
        var orderBook = api.getMarketDataService().getOrderBookSync(figi, depth);
        var asks = orderBook.getAsksList();
        var bids = orderBook.getBidsList();
        var lastPrice = quotationToBigDecimal(orderBook.getLastPrice());
        var closePrice = quotationToBigDecimal(orderBook.getClosePrice());
        log.info(
                "получен стакан по инструменту {}, глубина стакана: {}, количество предложений на покупку: {}, количество " +
                        "предложений на продажу: {}, цена последней сделки: {}, цена закрытия: {}",
                figi, depth, bids.size(), asks.size(), lastPrice, closePrice);

        log.info("предложения на покупку");
        for (Order bid : bids) {
            var price = quotationToBigDecimal(bid.getPrice());
            var quantity = bid.getQuantity();
            log.info("количество в лотах: {}, цена: {}", quantity, price);
        }

        log.info("предложения на продажу");
        for (Order ask : asks) {
            var price = quotationToBigDecimal(ask.getPrice());
            var quantity = ask.getQuantity();
            log.info("количество в лотах: {}, цена: {}", quantity, price);
        }
    }



    private static void getPortfolioExample(InvestApi api) {
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        //Получаем и печатаем портфолио
        var portfolio = api.getOperationsService().getPortfolioSync(mainAccount);
        var totalAmountBonds = portfolio.getTotalAmountBonds();
        log.info("общая стоимость облигаций в портфеле {}", totalAmountBonds);

        var totalAmountEtf = portfolio.getTotalAmountEtfs();
        log.info("общая стоимость фондов в портфеле {}", totalAmountEtf);

        var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
        log.info("общая стоимость валют в портфеле {}", totalAmountCurrencies);

        var totalAmountFutures = portfolio.getTotalAmountFutures();
        log.info("общая стоимость фьючерсов в портфеле {}", totalAmountFutures);

        var totalAmountShares = portfolio.getTotalAmountShares();
        log.info("общая стоимость акций в портфеле {}", totalAmountShares);

        log.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

        var positions = portfolio.getPositions();
        log.info("в портфолио {} позиций", positions.size());
        for (int i = 0; i < Math.min(positions.size(), 5); i++) {
            var position = positions.get(i);
            var figi = position.getFigi();
            var quantity = position.getQuantity();
            var currentPrice = position.getCurrentPrice();
            var expectedYield = position.getExpectedYield();
            log.info(
                    "позиция с figi: {}, количество инструмента: {}, текущая цена инструмента: {}, текущая расчитанная " +
                            "доходность: {}",
                    figi, quantity, currentPrice, expectedYield);
        }

    }

    private static void ordersServiceExample(InvestApi api, String figi) {
        //Выставляем заявку
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        var price = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();

        //Выставляем заявку на покупку по лимитной цене
        var orderId = api.getOrdersService()
                .postOrderSync(figi, 1, price, OrderDirection.ORDER_DIRECTION_BUY, mainAccount, OrderType.ORDER_TYPE_LIMIT,
                        UUID.randomUUID().toString()).getOrderId();

        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        var orders = api.getOrdersService().getOrdersSync(mainAccount);
        if (orders.stream().anyMatch(el -> orderId.equals(el.getOrderId()))) {
            log.info("заявка с id {} есть в списке активных заявок", orderId);
        }

        //Отменяем заявку
        api.getOrdersService().cancelOrder(mainAccount, orderId);
    }


    private static void instrumentsServiceExample(InvestApi api) {
        //Получаем базовые списки инструментов и печатаем их
        var shares = api.getInstrumentsService().getTradableSharesSync();
        var etfs = api.getInstrumentsService().getTradableEtfsSync();
        var bonds = api.getInstrumentsService().getTradableBondsSync();
        var futures = api.getInstrumentsService().getTradableFuturesSync();
        var currencies = api.getInstrumentsService().getTradableCurrenciesSync();

        //Для 3 акций выводим список событий по выплате дивидендов
        for (int i = 0; i < Math.min(shares.size(), 3); i++) {
            var share = shares.get(i);
            var figi = share.getFigi();
            var dividends =
                    api.getInstrumentsService().getDividendsSync(figi, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
            for (Dividend dividend : dividends) {
                log.info("дивиденд для figi {}: {}", figi, dividend);
            }
        }

        //Для 3 облигаций выводим список НКД
        for (int i = 0; i < Math.min(bonds.size(), 3); i++) {
            var bond = bonds.get(i);
            var figi = bond.getFigi();
            var accruedInterests = api.getInstrumentsService()
                    .getAccruedInterestsSync(figi, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
            for (AccruedInterest accruedInterest : accruedInterests) {
                log.info("НКД для figi {}: {}", figi, accruedInterest);
            }
        }

        //Для 3 фьючерсов выводим размер обеспечения
        for (int i = 0; i < Math.min(futures.size(), 3); i++) {
            var future = futures.get(i);
            var figi = future.getFigi();
            var futuresMargin = api.getInstrumentsService().getFuturesMarginSync(figi);
            log.info("гарантийное обеспечение при покупке для figi {}: {}", figi,
                    moneyValueToBigDecimal(futuresMargin.getInitialMarginOnBuy()));
            log.info("гарантийное обеспечение при продаже для figi {}: {}", figi,
                    moneyValueToBigDecimal(futuresMargin.getInitialMarginOnSell()));
            log.info("шаг цены figi для {}: {}", figi, quotationToBigDecimal(futuresMargin.getMinPriceIncrement()));
            log.info("стоимость шага цены для figi {}: {}", figi,
                    quotationToBigDecimal(futuresMargin.getMinPriceIncrementAmount()));
        }

        //Получаем время работы биржи
        var tradingSchedules =
                api.getInstrumentsService().getTradingScheduleSync("spb", Instant.now(), Instant.now().plus(5, ChronoUnit.DAYS));
        for (TradingDay tradingDay : tradingSchedules.getDaysList()) {
            var date = timestampToString(tradingDay.getDate());
            var startDate = timestampToString(tradingDay.getStartTime());
            var endDate = timestampToString(tradingDay.getEndTime());
            if (tradingDay.getIsTradingDay()) {
                log.info("расписание торгов для площадки SPB. Дата: {},  открытие: {}, закрытие: {}", date, startDate, endDate);
            } else {
                log.info("расписание торгов для площадки SPB. Дата: {}. Выходной день", date);
            }
        }

        //Получаем инструмент по его figi
        var instrument = api.getInstrumentsService().getInstrumentByFigiSync("BBG000B9XRY4");
        log.info(
                "инструмент figi: {}, лотность: {}, текущий режим торгов: {}, признак внебиржи: {}, признак доступности торгов " +
                        "через api : {}",
                instrument.getFigi(),
                instrument.getLot(),
                instrument.getTradingStatus().name(),
                instrument.getOtcFlag(),
                instrument.getApiTradeAvailableFlag());


        //Проверяем вывод ошибки в лог
        //Проверяем, что будет ошибка 50002. Об ошибках и причинах их возникновения - https://tinkoff.github.io/investAPI/errors/
        var bondFigi = bonds.get(0).getFigi(); //инструмент с типом bond
        try {
            api.getInstrumentsService().getCurrencyByFigiSync(bondFigi);
        } catch (ApiRuntimeException e) {
            log.info(e.toString());
        }

        //Получаем информацию о купонах облигации
        var bondCoupons = api.getInstrumentsService().getBondCouponsSync(bondFigi, Instant.now().minus(30, ChronoUnit.DAYS), Instant.now());
        for (Coupon bondCoupon : bondCoupons) {
            var couponDate = bondCoupon.getCouponDate();
            var couponType = bondCoupon.getCouponType().getDescriptorForType();
            var payment = moneyValueToBigDecimal(bondCoupon.getPayOneBond());
            log.info("выплаты по купонам. дата: {}, тип: {}, выплата на 1 облигацию: {}", couponDate, couponType, payment);
        }

        //Получаем список активов
        var assets = api.getInstrumentsService().getAssetsSync().stream().limit(5).collect(Collectors.toList());
        for (Asset asset : assets) {
            log.info("актив. uid : {}, имя: {}, тип: {}", asset.getUid(), asset.getName(), asset.getType());
        }

        //Получаем подробную информацию о активе
        var uid = assets.get(0).getUid();
        var assetBy = api.getInstrumentsService().getAssetBySync(uid);
        log.info("подробная информация об активе. описание: {}, статус: {}, бренд: {}", assetBy.getDescription(), assetBy.getStatus(), assetBy.getBrand().getInfo());

        //Добавление избранных инструментов
        var instruments = currencies.stream().map(Currency::getFigi).collect(Collectors.toList());
        var favoriteInstruments = api.getInstrumentsService().addFavoritesSync(instruments);

        //Удаление из списка избранных инструментов
        favoriteInstruments = api.getInstrumentsService().deleteFavoritesSync(List.of(currencies.get(0).getFigi()));
    }
}
