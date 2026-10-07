package com.moneybook.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSurroundingWhitespaceValidator implements ConstraintValidator<NoSurroundingWhitespace, CharSequence> {
    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || value.toString().equals(value.toString().strip());
    }
}
