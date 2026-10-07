package com.moneybook.backend.recovery;

import org.hibernate.exception.ConstraintViolationException;

/** Identifies only the verified email unique-index violation for safe domain error translation. */
public final class VerifiedEmailConstraint {
    public static final String INDEX_NAME = "uq_user_auth_verified_email";

    private VerifiedEmailConstraint() {
    }

    public static boolean wasViolated(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && INDEX_NAME.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
