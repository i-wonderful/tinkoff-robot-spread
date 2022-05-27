package com.byby.trobot.common;

/**
 * Каналы в eventBus
 */
public class GlobalBusAddress {
    // Для UI: Вывод основного лога
    public static final String LOG = "LOG";
    // Для UI: Лог текущих открытых заявок
    public static final String LOG_ORDER = "LOG_ORDER";
    // Для UI: Лог некритичных ошибок
    public static final String LOG_ERROR = "LOG_ERROR";
    // Для UI: Лог критических ошибок, мешающих продолжать работу
    public static final String LOG_ERR_CRITICAL = "LOG_ERR_CRITICAL";
    // Акции для которых запущен робот в данный момент
    public static final String LOG_CURRENT_RUN_TICKERS = "LOG_CURRENT_RUN_TICKERS";
    // Канал для отправки новых найденных figi подходящих акций
    public static final String NEW_FIGI = "NEW_FIGI";
}
