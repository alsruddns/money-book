package com.moneybook.backend.moneybook.dto;

public record MoneyBookOwnerTransferResponse(Long moneyBookUid, Long previousOwnerUserUid,
                                             Long ownerUserUid) { }
