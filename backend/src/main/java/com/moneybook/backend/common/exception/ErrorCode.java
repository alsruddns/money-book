package com.moneybook.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MONEY_BOOK_BACKUP_FORBIDDEN(HttpStatus.FORBIDDEN, "가계부 소유자 또는 관리자만 백업할 수 있습니다."),
    BACKUP_FILE_INVALID(HttpStatus.BAD_REQUEST, "백업 파일 형식이나 내용이 올바르지 않습니다."),
    BACKUP_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "백업 파일 크기는 20MB 이하여야 합니다."),
    BACKUP_VERSION_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 백업 버전입니다."),
    BACKUP_REFERENCE_INVALID(HttpStatus.BAD_REQUEST, "백업 내부 참조가 올바르지 않습니다."),
    BACKUP_DUPLICATE_IDENTIFIER(HttpStatus.BAD_REQUEST, "백업 내부 식별자가 중복되었습니다."),
    BACKUP_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "백업 파일을 생성할 수 없습니다."),
    BACKUP_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "백업을 복원할 수 없습니다."),
    MONTH_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 마감된 월입니다."),
    MONTH_NOT_CLOSED(HttpStatus.NOT_FOUND, "마감되지 않은 월입니다."),
    MONTH_CLOSED(HttpStatus.CONFLICT, "마감된 월의 거래, 이체 또는 예산을 변경할 수 없습니다."),
    FUTURE_MONTH_CLOSING(HttpStatus.BAD_REQUEST, "미래 월은 마감할 수 없습니다."),
    BUDGET_CATEGORY_SUM_EXCEEDED(HttpStatus.BAD_REQUEST, "카테고리 예산 합계가 총예산을 초과합니다."),
    BUDGET_CATEGORY_NOT_EXPENSE(HttpStatus.BAD_REQUEST, "지출 카테고리에만 예산을 설정할 수 있습니다."),
    RECURRING_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "정기 거래 규칙을 찾을 수 없습니다."),
    INVALID_RECURRING_DAY(HttpStatus.BAD_REQUEST, "정기 거래 주기와 예정일 설정이 올바르지 않습니다."),
    INVALID_RECURRING_DATE_RANGE(HttpStatus.BAD_REQUEST, "정기 거래 시작일과 종료일이 올바르지 않습니다."),
    RECURRING_GENERATION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "한 번에 생성할 수 있는 정기 거래 수를 초과했습니다."),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "이체를 찾을 수 없습니다."),
    SAME_TRANSFER_ACCOUNT(HttpStatus.BAD_REQUEST, "출금 계좌와 입금 계좌가 같을 수 없습니다."),
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
