## Документация Tinkoff Robot Spread

### Описание настроек в application.properties

#### Настройки робота
`robot.appname` - Appname InvestApi <br/>
`robot.sandbox.mode` - Режим песочницы: true/false<br/>
`robot.token.sandbox` - Токен песочницы. Необязательный параметр. Можно не указывать, если работаем с реальным счетом.<br/>
`robot.token.real` - Токен реального счета. Необязательный параметр. Можно не указывать, если работаем со счетом песочницы.<br/> 
`robot.sandbox.init.balance.usd` - Начальный баланс песочницы в долларах при пересоздании аккаунта, число double.<br/>
`robot.sandbox.init.balance.rub` - Начальный баланс песочницы в рублях при пересоздании аккаунта, число double.<br/>
`robot.exchange.names`- Биржи с которыми работаем. Обязательный параметр.<br/>
`robot.margin.allow` - Доступна ли маржинальная торговля. boolean<br/>

#### Настройки поиска
`robot.strategy.shares.tickers.find` - Список акций которыми будем торговать. Тикеры через запятую. Необязательный параметр, если не указывать, будет поиск по всем. <br/>
`robot.strategy.shares.tickers.exclude` - Акции которые исключить из поиска. Тикеры через запятую. Необязательный параметр.<br/>
`robot.strategy.shares.max.count` - Количество акций которыми будем торговать. <br/>

#### Настройки стратегии
`robot.strategy.shares.spread.percent` - Минимальный величина спреда при которой выставляем заявки. В проценте от цены акции.<br/>
`robot.strategy.shares.count.one.minute` - Число акций обрабатываемых в минуту при поиске по всем акциям. Чтобы избежать превышения лимитов.<br/>
`robot.strategy.shares.price.max.usd` - Торгуем акциями не дороже цены, в долларах.<br/>
`robot.strategy.shares.price.max.rub` - Торгуем акциями не дороже цены, в рублях.<br/>

### Описание классов и пакетов.

[`com.byby.trobot.strategy`](/strategy.md) - классы стратегии и управления<br/>
`com.byby.trobot.cache` - управление кешем<br/>
`com.byby.trobot.common` - глобальные константы, общие классы<br/>
`com.byby.trobot.config` - конфиг, инициализация<br/>
`com.byby.trobot.controller` - контроллеры<br/>
`com.byby.trobot.db` - работа с бд<br/>
`com.byby.trobot.executor` - исполнители операции с песочницей или реальным счетом<br/>
`com.byby.trobot.service` - сервисы работы с tinkoff api<br/>


to be continued...





