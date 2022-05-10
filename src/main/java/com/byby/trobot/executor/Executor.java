package com.byby.trobot.executor;

import com.byby.trobot.dto.PortfolioDto;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;

public interface Executor {
    String getAccountId();
    PostOrderResponse postBuyOrder(String figi);
    void cancelBuyOrder(String figi);
    PortfolioDto getPortfolio();
}
