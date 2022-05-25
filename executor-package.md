## Классы работы в песочницей или реальным счетом.

`com.byby.trobot.executor.*`

### Executor

Унифицирована через интерфейс работа с реальным счетом и песочницы.
Конкретная реализация загружается в зависимостиот флага `robot.sandbox.mode` в настройках.

- `isMyBuyOrderOptimal(OrderState myOrderBuy, Order bidFromOrderbook)` - Является ли моя заявка на покупку оптимальной.
- `isMySellOrderOptimal(OrderState myOrderSell, Order askFromOrderbook)` - Является ли моя заявка на продажу
  оптимальной.
- `postBuyLimitOrder(String figi, BigDecimal price)` - Выставить лимитную заявку на покупку.
- `postSellLimitOrder(String figi, BigDecimal price)` - Выставить лимитную заявку на продажу.
- `cancelAllOrders()` - Отменить все лимитные заявки
- `cancelOrder(String orderId)` - Отменить существующую заявку
- `getPortfolio()` - Получить портфолио
- `getMyOrders()` - Получить список активных заявок по счёту.
- `loadAccountId()` - Загрузить accountId.