package com.moneybook.backend.recovery;

import java.util.Locale;

/** Applies the canonical form used for verified account recovery email addresses. */
public final class EmailAddressNormalizer {
    private EmailAddressNormalizer() {
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
