package com.moneybook.backend.recovery.dto;
import com.moneybook.backend.enums.SecurityQuestionCode;
public record SecurityQuestionResponse(SecurityQuestionCode code,String question) { }
