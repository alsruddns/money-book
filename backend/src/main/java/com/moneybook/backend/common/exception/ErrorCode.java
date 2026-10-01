package com.moneybook.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MONEY_BOOK_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부에 등록할 권한이 없습니다."),
    MONEY_BOOK_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부를 조회할 권한이 없습니다."),
    MONEY_BOOK_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부를 수정할 권한이 없습니다."),
    MONEY_BOOK_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부에서 삭제할 권한이 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "같은 유형의 카테고리 이름이 이미 있습니다."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "거래에서 사용 중인 카테고리는 삭제할 수 없습니다."),
    CATEGORY_NOT_IN_MONEY_BOOK(HttpStatus.BAD_REQUEST, "카테고리가 해당 가계부에 속하지 않습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "같은 이름의 계좌가 이미 있습니다."),
    ACCOUNT_IN_USE(HttpStatus.CONFLICT, "거래에서 사용 중인 계좌는 삭제할 수 없습니다."),
    ACCOUNT_NOT_IN_MONEY_BOOK(HttpStatus.BAD_REQUEST, "계좌가 해당 가계부에 속하지 않습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."),
    TRANSACTION_CATEGORY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "거래 유형과 카테고리 유형이 다릅니다."),
    MONEY_BOOK_MEMBERS_VIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부 멤버를 조회할 권한이 없습니다."),
    MONEY_BOOK_MEMBER_PERMISSION_FORBIDDEN(HttpStatus.FORBIDDEN, "멤버 권한을 수정할 권한이 없습니다."),
    MONEY_BOOK_MEMBER_REMOVAL_FORBIDDEN(HttpStatus.FORBIDDEN, "멤버를 제거할 권한이 없습니다."),
    MONEY_BOOK_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MONEY_BOOK_MEMBER_NOT_ACCEPTED(HttpStatus.CONFLICT, "가입된 멤버만 관리할 수 있습니다."),
    MONEY_BOOK_OWNER_PERMISSION_PROTECTED(HttpStatus.FORBIDDEN, "소유자의 권한은 수정할 수 없습니다."),
    MONEY_BOOK_OWNER_REMOVAL_PROTECTED(HttpStatus.FORBIDDEN, "소유자는 제거할 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인 값이 일치하지 않습니다."),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 올바르지 않습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "활성 상태의 사용자만 로그인할 수 있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 유효하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    MONEY_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "가계부를 찾을 수 없습니다."),
    INVITEE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대할 LOCAL 사용자를 찾을 수 없습니다."),
    SELF_INVITATION(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없습니다."),
    MONEY_BOOK_INVITATION_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부 사용자를 초대할 권한이 없습니다."),
    ALREADY_MONEY_BOOK_MEMBER(HttpStatus.CONFLICT, "이미 가입된 사용자입니다."),
    INVITATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 대기 중인 초대가 있습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    INVITATION_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 초대만 처리할 수 있습니다."),
    INVITATION_NOT_PENDING(HttpStatus.CONFLICT, "대기 중인 초대만 처리할 수 있습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
