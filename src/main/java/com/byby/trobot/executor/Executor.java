package com.byby.trobot.executor;

import com.byby.trobot.dto.PortfolioDto;
import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

import java.math.BigDecimal;
import java.util.List;

public interface Executor {
    String getAccountId();
    void cancelOrder(String orderId);
    PortfolioDto getPortfolio();

    /**
     * Метод получения списка активных заявок по счёту.
     */
    Uni<List<OrderState>> getMyOrders();

    /**
     * Выставить лимитныу заявку на покупку по оптимальной цене (на шаг дороже существуюущей)
     */
    PostOrderResponse postBuyLimitOrder(String figi, BigDecimal price);

    /**
     * Выставить лимитную заявку на продажу по лучшей цене (на шаг дешевле существующей)
     */
    PostOrderResponse postSellLimitOrder(String figi);
}
