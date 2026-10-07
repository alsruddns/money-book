package com.moneybook.backend.account.dto;

import com.moneybook.backend.entity.MoneyBookAccount;
import com.moneybook.backend.enums.AccountType;

public record AccountResponse(Long accountUid, Long moneyBookUid, String name,
                              AccountType accountType, int sortOrder) {
    public static AccountResponse from(MoneyBookAccount account) {
        return new AccountResponse(account.getAccountUid(), account.getMoneyBook().getMoneyBookUid(),
                account.getName(), account.getAccountType(), account.getSortOrder());
    }
}
