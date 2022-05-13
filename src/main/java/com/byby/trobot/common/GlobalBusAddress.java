package com.byby.trobot.common;

/**
 * Каналы в eventBus
 */
public class GlobalBusAddress {
    // Вывод основного лога в ui
    public static final String LOG = "LOG";
    // Лог текущих заявок
    public static final String LOG_ORDER = "LOG_ORDER";
    // Лог ошибок в ui
    public static final String LOG_ERR = "LOG_ERR";
    // Выставить заявку на покупу
    public static final String POST_BUY_ORDER = "POST_BUY_ORDER";
    // Выставить заяввку на продажу
    public static final String POST_SELL_ORDER = "POST_SELL_ORDER";
    // Отменить заявку
    public static final String CANCEL_ORDER = "CANCEL_BUY_ORDER";

}
