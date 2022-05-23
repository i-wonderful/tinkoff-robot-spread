# Tinkoff Spread Robot

Робот трогующий на спредах. 

## Запуск. 

### 1. Запустить бд у докере:
```shell script
docker run -it --rm=true \
    --name postgres-quarkus -e POSTGRES_USER=trobot \
    -e POSTGRES_PASSWORD=trobot -e POSTGRES_DB=trobot \
    -p 5433:5432 postgres:14.1
```
### 2. Сборка и запуск: 
```shell script
./mvnw package
```
```shell script
java -jar target/quarkus-app/quarkus-run.jar
```
Открыть в браузере: http:localhost:8081

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:



It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using ``.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/tinkof-robot-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A JAX-RS implementation utilizing build time
  processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions
  that depend on it.
- Reactive Routes ([guide](https://quarkus.io/guides/reactive-routes)): REST framework offering the route model to
  define non blocking endpoints

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
