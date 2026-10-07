package com.moneybook.backend.board.provider;

import org.springframework.stereotype.Component;

/** Converts a stored nickname to the only author label exposed by public Board responses. */
@Component
public class BoardNicknameMasker {

    public String mask(String nickname) {
        if (nickname == null || nickname.isEmpty()) return "*";
        int[] codePoints = nickname.codePoints().toArray();
        if (codePoints.length == 1) return "*";
        if (codePoints.length == 2) return new String(codePoints, 0, 1) + "*";
        return new String(codePoints, 0, 1) + "*".repeat(codePoints.length - 2)
                + new String(codePoints, codePoints.length - 1, 1);
    }
}
