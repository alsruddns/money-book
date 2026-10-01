package com.moneybook.backend.transfer.service;

import com.moneybook.backend.transfer.dto.CreateTransferRequest;
import com.moneybook.backend.transfer.dto.TransferResponse;
import com.moneybook.backend.transfer.dto.UpdateTransferRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TransferService {
    TransferResponse create(Long moneyBookUid, CreateTransferRequest request, Authentication authentication);
    TransferResponse detail(Long moneyBookUid, Long transferUid, Authentication authentication);
    TransferResponse update(Long moneyBookUid, Long transferUid, UpdateTransferRequest request,
                            Authentication authentication);
    void delete(Long moneyBookUid, Long transferUid, Authentication authentication);
    List<TransferResponse> list(Long moneyBookUid, int year, int month, Authentication authentication);
}
