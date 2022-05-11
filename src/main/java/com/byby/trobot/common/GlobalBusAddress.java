package com.byby.trobot.common;

/**
 * Канала в eventBus
 */
public enum GlobalBusAddress {
    // Вывод основного лога в ui
    LOG,
    // Лог ошибок в ui
    LOG_ERR,
    // Выставить заяввку на продажу
    POST_SELL_ORDER;

    // Выставить заявку на покупу
    public static final String POST_BUY_ORDER = "POST_BUY_ORDER";
    public static final String BUY_ORDER = "CANCEL_BUY_ORDER";
}
