# Tinkoff Spread Robot

Робот торгующий на спредах. 

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
Открыть в браузере: http:localhost:8081/

> **_NOTE:_**  Можно запустить в дев режиме: ./mvnw compile quarkus:dev

## Описание работы

При размере спреда больше определенной величины, выставляем две заявки на продажу и покупку на шаг цены отличающиеся 
от существующих в стакане. Для быстрого реагирования подписываемся на изменение orderbook. Если пред слишком меленький, 
заявки убираем. Если спред изменился, а заявки уже существуют, проверяем их оптимальность: они должны быть на вершине стакана. 

Если знаем какими акциями хотим торговать, указываем их тикеры в файле настроек ./main/resources/application.properties 
в параметре robot.strategy.shares.tickers.find через запятую. Если не знаем какими акциями будем торговать, оставляем это поле пустым.
В этом случае запуститься поиск по всем акциям, отбирающих нужные акции по величине спреда и цене. 
Величина спреда указывается в настройках, параметр robot.strategy.shares.spread.percent число double. 
Это процент которым является абсолютная величина спреда к цене самое акции (ask price в данном случае). Как правила это небольшая величина порядка 0.7.
Так же если не хотим торговать слишком дорогими акциями, указываем максимальную цену 
в параметрах robot.strategy.shares.price.max.usd и robot.strategy.shares.price.max.rub.

Чтобы поиск по всем акциям не вызывал превышения лимитов запросов, ищем их партиями. 
В одну минуту запускается таймер, где обрабатывается следующие robot.strategy.shares.count.one.minute количество акций (считаем спреды акций и оставляем нужные).
Когда нашли очередную порцию акций, помещаем их в кеш, где хранятся все интересующие нас акции, перезапускаем работающую в данный момент стратегию.
Старатегия запускается когда мы нашли первую партию подходящих акций. 
Количество акций которыми мы хотим торговать определяется параметром robot.strategy.shares.max.count. 
Когда мы нашли нужное число акций, таймеры останавливаются, больше ничего не ищем. 

В стратегии подписываемся на стрим изменения стакана и стрим сделок. 
Обрабатываем данные стакана, решаем выставлять ли заявки или убирать. Если выставлять проверяем есть ли они и на вершине ли стакана.
Когда сделка совершена, записываем данные в бд. 
В бд для статистики храним пользовательский сеанс (время начала и завершения работы) и сделки в это время.

Логгирование осуществляется асинхронними отправками сообщений. Лог выводится в ui. Более подробный лог в консоль и файл trobot.log.

Чтобы это все благополучно работало, задействовано максимум асинхронных вызовов. Все написано на реактивной библ. smallrye-mutiny 
и других реактивных расширениях фреймворка quarkus. Сообщения передаются в vertx eventBus. На фронте они выводятся в реалтайме.

В планах расширить критерии поиска акций, сделать остановку робота по таймеру, 
улучшить ui, улучшить eventHandler, доработать логику купли-продажи, потестить нейтив сборку.

p.s сейчас указан appname=i-wonderful и с ним тестировалось последний торговый день, 
ранее был ошибочно указан appname=iwonderful, если можно посмотрите те заявки тоже.



                                     