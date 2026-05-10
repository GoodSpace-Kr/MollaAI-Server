package com.molla.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ──────────────────────────────────────────────
    // 인증 / 토큰
    // ──────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token을 찾을 수 없습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다."),

    // ──────────────────────────────────────────────
    // SMS 인증
    // ──────────────────────────────────────────────
    VERIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_NOT_FOUND", "인증번호를 찾을 수 없습니다. 다시 요청해주세요."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_MISMATCH", "인증번호가 일치하지 않습니다."),
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SMS_SEND_FAILED", "SMS 발송에 실패했습니다."),

    // ──────────────────────────────────────────────
    // 유저
    // ──────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "유저를 찾을 수 없습니다."),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "USER_ALREADY_REGISTERED", "이미 가입된 유저입니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "정지된 계정입니다."),

    // ──────────────────────────────────────────────
    // 구독
    // ──────────────────────────────────────────────
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."),
    SUBSCRIPTION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "SUBSCRIPTION_ALREADY_ACTIVE", "이미 활성화된 구독이 있습니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "DAILY_LIMIT_EXCEEDED", "오늘의 통화 가능 시간을 초과했습니다."),

    // ──────────────────────────────────────────────
    // 통화 세션
    // ──────────────────────────────────────────────
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "통화 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "SESSION_ALREADY_ENDED", "이미 종료된 통화 세션입니다."),

    // ──────────────────────────────────────────────
    // 리포트
    // ──────────────────────────────────────────────
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "리포트를 찾을 수 없습니다."),
    REPORT_NOT_READY(HttpStatus.ACCEPTED, "REPORT_NOT_READY", "리포트가 아직 생성 중입니다."),

    // ──────────────────────────────────────────────
    // 단어장
    // ──────────────────────────────────────────────
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK_NOT_FOUND", "단어장 항목을 찾을 수 없습니다."),
    UNAUTHORIZED_BOOKMARK_ACCESS(HttpStatus.FORBIDDEN, "UNAUTHORIZED_BOOKMARK_ACCESS", "본인의 단어장만 삭제할 수 있습니다."),

    // ──────────────────────────────────────────────
    // 공통
    // ──────────────────────────────────────────────
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
