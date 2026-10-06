package com.moneybook.backend.board;

import com.moneybook.backend.board.provider.BoardNicknameMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardNicknameMaskerTests {
    private final BoardNicknameMasker masker = new BoardNicknameMasker();

    @Test
    void masksOneAndTwoCodePointNames() {
        assertEquals("*", masker.mask("김"));
        assertEquals("민*", masker.mask("민경"));
    }

    @Test
    void masksThreeAndLongerNamesForKoreanAndLatinText() {
        assertEquals("민*운", masker.mask("민경운"));
        assertEquals("홍**님", masker.mask("홍길동님"));
        assertEquals("a****f", masker.mask("abcdef"));
    }

    @Test
    void countsUnicodeCodePointsInsteadOfUtf16CodeUnits() {
        assertEquals("*", masker.mask("😀"));
        assertEquals("😀*", masker.mask("😀😃"));
        assertEquals("😀*🚀", masker.mask("😀😃🚀"));
    }
}
