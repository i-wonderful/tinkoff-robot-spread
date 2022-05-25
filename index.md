# Документация Tinkoff Robot Spread

Проект на java 11, сборка maven. 
Используется фреймворк [quarkus](https://quarkus.io) и его расширения: 
[mutiny](https://smallrye.io/smallrye-mutiny/), quarkus-hibernate-reactive-panache, quarkus-resteasy-reactive, quarkus-vertx и др. 
Бд postgresql, запускается в докере. ui на vuejs, взаимодействие с бекендом как по rest так и по eventbus.

## Запуск.
### 1. Указать токен
В файле настроек ./main/resources/application.properties
параметр robot.token.real и токен песочницы robot.token.sandbox.
Выставить флаг работы в песочнице robot.sandbox.modе желаемым образом.
### 2. Запустить бд в докере:
```shell script
docker run -it --rm=true \
    --name postgres-quarkus -e POSTGRES_USER=trobot \
    -e POSTGRES_PASSWORD=trobot -e POSTGRES_DB=trobot \
    -p 5433:5432 postgres:14.1
```
### 3. Сборка и запуск:
```shell script
./mvnw package
```
```shell script
java -jar ./target/quarkus-app/quarkus-run.jar
```
Открыть в браузере: http://localhost:8081/
 

## Описание настроек в application.properties

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

[com.byby.trobot.strategy](/strategy-package.md) - классы стратегии и управления<br/>
[com.byby.trobot.executor](/executor-package.md) - исполнители операции с песочницей или реальным счетом<br/>
`com.byby.trobot.cache` - управление кешем<br/>
`com.byby.trobot.common` - глобальные константы, общие классы<br/>
`com.byby.trobot.config` - конфиг, инициализация<br/>
`com.byby.trobot.controller` - контроллеры<br/>
`com.byby.trobot.db` - работа с бд<br/>

`com.byby.trobot.service` - сервисы работы с tinkoff api<br/>


to be continued...





