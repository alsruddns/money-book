package com.moneybook.backend.enums;

/** Privacy-safe recovery prompts; never add identifying or publicly discoverable questions. */
public enum SecurityQuestionCode {
    FAVORITE_FOOD("가장 좋아하는 음식은 무엇인가요?"),
    FAVORITE_WORK("가장 좋아하는 영화, 드라마, 애니메이션 또는 책의 제목은 무엇인가요?"),
    FAVORITE_COLOR("가장 좋아하는 색은 무엇인가요?"),
    FAVORITE_CHARACTER("가장 좋아하는 가상의 캐릭터 이름은 무엇인가요?"),
    RECOVERY_WORD("비밀번호 복구를 위해 정해둔 나만의 단어는 무엇인가요?");
    private final String question;
    SecurityQuestionCode(String question) { this.question = question; }
    public String question() { return question; }
}
