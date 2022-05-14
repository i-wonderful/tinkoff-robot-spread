package com.byby.trobot.executor;

import com.byby.trobot.dto.PortfolioDto;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Вызов песочницы или реальных сервисов,
 * в зависимости от настройки robot.sandbox.mode
 */
public interface Executor {
    String getAccountId();
    void cancelOrder(String orderId);
    Uni cancelAllOrders();

    /**
     * Получить портфолио
     */
    Uni<PortfolioDto> getPortfolio();

    /**
     * Получить список активных заявок по счёту.
     */
    Uni<List<OrderState>> getMyOrders();

    /**
     * Выставить лимитную заявку на покупку.
     */
    PostOrderResponse postBuyLimitOrder(String figi, BigDecimal price);

    /**
     * Выставить лимитную заявку на продажу.
     */
    PostOrderResponse postSellLimitOrder(String figi, BigDecimal price);

    /**
     *
     *
     * @param myOrderBuy моя выставленная заявка на покупку
     * @param bidFromOrderbook верхняя заявка на покупку из стакана
     * @return является ли моя заявка оптимальной
     */
    boolean isMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook);

    /**
     *
     * @param myOrderSell
     * @param askFromOrderbook
     * @return
     */
    boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook);
}
