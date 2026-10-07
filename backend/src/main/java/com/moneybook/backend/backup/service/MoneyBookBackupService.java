package com.moneybook.backend.backup.service;

import com.moneybook.backend.backup.dto.BackupRestoreResponse;
import com.moneybook.backend.backup.dto.BackupValidationResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.io.OutputStream;

public interface MoneyBookBackupService {
    void writeBackup(Long moneyBookUid, Authentication authentication, OutputStream output);
    BackupValidationResponse validate(MultipartFile file, Authentication authentication);
    BackupRestoreResponse restore(MultipartFile file, Authentication authentication);
}
