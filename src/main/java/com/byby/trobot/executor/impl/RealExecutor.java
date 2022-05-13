package com.byby.trobot.executor.impl;

import com.byby.trobot.dto.PortfolioDto;
import com.byby.trobot.executor.Executor;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;

//@Startup
@LookupIfProperty(name = "robot.sandbox.mode", stringValue = "false")
@ApplicationScoped
public class RealExecutor implements Executor {
    private static final Logger log = LoggerFactory.getLogger(RealExecutor.class);

    private InvestApi api;

    public RealExecutor(InvestApi api) {
        log.info(">>> Init Real Executor");
        this.api = api;
    }

    @Override
    public String getAccountId() {
        // todo
        return null;
    }

    @Override
    public PostOrderResponse postBuyLimitOrder(String figi, BigDecimal price) {
        log.info(">>> todo. Real. Выставить заявку на покупку");
        // todo
        return null;
    }

    @Override
    public PostOrderResponse postSellLimitOrder(String figi, BigDecimal price) {
        log.info(">>> todo. Real. Post Sell Order");
        return null;
    }

    @Override
    public boolean isMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook) {
        if (bidFromOrderbook == null) {
            log.warn(">>> Bid form orderbook is null.");
            return true;
        }
        MoneyValue myPrice = myOrderBuy.getInitialSecurityPrice();
        Quotation orderbookPrice = bidFromOrderbook.getPrice();
        boolean isEquals =
                myPrice.getUnits() == orderbookPrice.getUnits() &&
                        myPrice.getUnits() == orderbookPrice.getNano();
        return isEquals;
    }

    @Override
    public boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook) {
        // todo
        log.info(">>> isMySellOrderOptimal Real ");
        return false;
    }

    @Override
    public void cancelOrder(String orderId) {
        log.info(">>> cancelBuyOrder Real ");
    }

    @Override
    public Uni cancelAllOrders() {
        return Uni.createFrom().voidItem();
    }

    @Override
    public PortfolioDto getPortfolio() {
        // todo
        return null;
    }

    @Override
    public Uni<List<OrderState>> getMyOrders() {
        return Uni.createFrom()
                .completionStage(api.getOrdersService().getOrders(getAccountId()));
    }
}
