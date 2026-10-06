package com.moneybook.backend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = NoSurroundingWhitespaceValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSurroundingWhitespace {
    String message() default "보안 질문 답변의 앞뒤에는 공백을 입력할 수 없습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
