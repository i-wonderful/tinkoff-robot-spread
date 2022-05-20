package com.byby.trobot.executor.impl;

import io.smallrye.mutiny.Uni;
import ru.tinkoff.piapi.contract.v1.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Helper {

    /**
     * Найти открытые аккаунты среди списка.
     *
     * @param accounts список всех аккаутов
     * @return accountId
     */
    protected static Optional<String> findOpenAccountId(List<Account> accounts) {
        return accounts.stream()
                .filter(account -> AccountStatus.ACCOUNT_STATUS_OPEN.equals(account.getStatus()))
                .filter(account -> AccessLevel.ACCOUNT_ACCESS_LEVEL_FULL_ACCESS.equals(account.getAccessLevel()))
                .findFirst()
                .map(Account::getId);
    }

    protected static boolean isEqual(MoneyValue myPrice, Quotation orderbookPrice) {
        return myPrice.getUnits() == orderbookPrice.getUnits() &&
                myPrice.getNano() == orderbookPrice.getNano();
    }

    protected static <T> Uni<T> toUni(CompletableFuture<T> completableFuture) {
        return Uni.createFrom()
                .completionStage(completableFuture);
    }
}
