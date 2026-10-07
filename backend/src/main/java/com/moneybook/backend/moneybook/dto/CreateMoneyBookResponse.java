package com.moneybook.backend.moneybook.dto;

public record CreateMoneyBookResponse(Long moneyBookUid, String name, Long ownerUserUid) {
}
