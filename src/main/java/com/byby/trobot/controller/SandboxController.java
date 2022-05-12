package com.byby.trobot.controller;

import com.byby.trobot.config.ApplicationProperties;
import com.byby.trobot.service.impl.OrderbookService;
import com.byby.trobot.service.impl.SharesService;
import com.byby.trobot.strategy.impl.model.Spread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/sandbox")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SandboxController {
    private static final Logger log = LoggerFactory.getLogger(SandboxService.class);

    @Inject
    SharesService sharesService;

    @Inject
    OrderbookService orderbookService;

    @Inject
    ApplicationProperties properties;


//    @GET
//    @Path("/go")
//    public Response go(){
//        List<String> openExchanges = properties.getRobotExchangeNames();
//        List<Share> shares = sharesService.getShares(openExchanges);
//
//        List<Spread> spread = sp.getSpreads(shares);
//        return Response.ok(spread).build();
//    }


    // инфа о юзере, не песочница
    private static void usersServiceExample(InvestApi api) {
        //Получаем список аккаунтов и распечатываем их с указанием привилегий токена
        System.out.println(">>> Sandbox " + api.isSandboxMode());
        if (api.isSandboxMode()) {


            var accounts = api.isSandboxMode() ? api.getSandboxService().getAccountsSync() : api.getUserService().getAccountsSync();
            var mainAccount = accounts.get(0);
            for (Account account : accounts) {
                log.info("account id: {}, access level: {}", account.getId(), account.getAccessLevel().name());
            }

            //Получаем и печатаем информацию об обеспеченности портфеля
//            var marginAttributes = api.getUserService().getMarginAttributesSync(mainAccount.getId());
//            log.info("Ликвидная стоимость портфеля: {}", moneyValueToBigDecimal(marginAttributes.getLiquidPortfolio()));
//            log.info("Начальная маржа — начальное обеспечение для совершения новой сделки: {}",
//                    moneyValueToBigDecimal(marginAttributes.getStartingMargin()));
//            log.info("Минимальная маржа — это минимальное обеспечение для поддержания позиции, которую вы уже открыли: {}",
//                    moneyValueToBigDecimal(marginAttributes.getMinimalMargin()));
//            log.info("Уровень достаточности средств. Соотношение стоимости ликвидного портфеля к начальной марже: {}",
//                    quotationToBigDecimal(marginAttributes.getFundsSufficiencyLevel()));
//            log.info("Объем недостающих средств. Разница между стартовой маржой и ликвидной стоимости портфеля: {}",
//                    moneyValueToBigDecimal(marginAttributes.getAmountOfMissingFunds()));

            return;
        }

        //Получаем и печатаем информацию о текущих лимитах пользователя
        var tariff = api.getUserService().getUserTariffSync();
        log.info("stream type: marketdata, stream limit: {}", tariff.getStreamLimitsList().get(0).getLimit());
        log.info("stream type: orders, stream limit: {}", tariff.getStreamLimitsList().get(1).getLimit());
        log.info("current unary limit per minute: {}", tariff.getUnaryLimitsList().get(0).getLimitPerMinute());


    }

    //public Response getSandbox(){}
}
