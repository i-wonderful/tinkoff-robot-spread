package com.byby.trobot.executor;

import com.byby.trobot.controller.dto.PortfolioDto;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.Order;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Операции песочницы или реальных сервисов.
 *
 * Имплементация инициализируется в зависимости от настройки robot.sandbox.mode
 */
public interface Executor {

    /**
     * Отменить существующую заявку
     *
     * @param orderId айди заявки
     * @return время отмены
     */
    Uni<Instant> cancelOrder(String orderId);

    /**
     * Отменить все лимитные заявки
     */
    Uni<Void> cancelAllOrders();

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
    Uni<PostOrderResponse> postBuyLimitOrder(String figi, BigDecimal price);

    /**
     * Выставить лимитную заявку на продажу.
     */
    Uni<PostOrderResponse> postSellLimitOrder(String figi, BigDecimal price);

    /**
     * @param myOrderBuy       моя выставленная заявка на покупку
     * @param bidFromOrderbook верхняя заявка на покупку из стакана
     * @return является ли моя заявка оптимальной
     */
    boolean isMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook);

    /**
     * @param myOrderSell
     * @param askFromOrderbook
     * @return
     */
    boolean isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook);

    /**
     * Загрузить accountId.
     *
     * @return
     */
    Uni<String> loadAccountId();
}
