package com.byby.trobot.common;

/**
 * Канала в eventBus
 */
public enum GlobalBusAddress {
    // Вывод основного лога в ui
    LOG,
    // Лог ошибок в ui
    LOG_ERR,
    // Выставить заявку на покупу
    POST_BUY_ORDER,
    // Выставить заяввку на продажу
    POST_SELL_ORDER;

    public static final String portBuyOrder = "POST_BUY_ORDER";
    public static final String cancelBuyOrder = "CANCEL_BUY_ORDER";
}
