package com.byby.trobot.service;

import io.smallrye.mutiny.Uni;

/**
 * Операции которые можно сделать только в песочнице.
 */
public interface SandboxAccountService {
    /**
     * Пересоздать аккаунт песочницы,
     * пополнить счет суммами заданными в настройках:
     * robot.sandbox.init.balance.usd,
     * robot.sandbox.init.balance.rub
     *
     * @return accountId нового аккаунта
     */
    Uni<String> recreateSandbox();
}
