package com.moneybook.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.MultipartConfigElement;

/** Bounds multipart memory/disk use for uploaded MoneyBook backup files. */
@Configuration
public class BackupUploadConfig {
    @Bean
    public MultipartConfigElement backupMultipartConfig() {
        return new MultipartConfigElement("", 20L * 1024 * 1024,
                21L * 1024 * 1024, 256 * 1024);
    }
}
