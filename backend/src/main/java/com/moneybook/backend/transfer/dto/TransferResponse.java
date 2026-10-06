package com.moneybook.backend.transfer.dto;

import com.moneybook.backend.entity.MoneyBookTransfer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferResponse(
        Long transferUid,
        Long moneyBookUid,
        Long fromAccountUid,
        String fromAccountName,
        Long toAccountUid,
        String toAccountName,
        BigDecimal amount,
        LocalDate transferDate,
        String memo
) {
    public static TransferResponse from(MoneyBookTransfer transfer) {
        return new TransferResponse(transfer.getTransferUid(), transfer.getMoneyBook().getMoneyBookUid(),
                transfer.getFromAccount().getAccountUid(), transfer.getFromAccount().getName(),
                transfer.getToAccount().getAccountUid(), transfer.getToAccount().getName(),
                transfer.getAmount(), transfer.getTransferDate(), transfer.getMemo());
    }
}
