package com.api.common.code;

/**
 * user-service 성공 응답 메타정보를 정의합니다.
 */
public enum SuccessCode {
    /** 내 정보 조회 성공 */
    USER_ME_GET_SUCCESS(200, 3000, "내 사용자 정보 조회 성공"),

    /** 회원가입 성공 */
    USER_SIGNUP_SUCCESS(201, 3001, "회원가입 성공"),

    /** 내부 사용자 생성 성공 */
    USER_CREATE_SUCCESS(201, 3002, "사용자 생성 성공"),

    /** 사용자 상태 변경 성공 */
    USER_STATUS_UPDATE_SUCCESS(200, 3003, "사용자 상태 변경 성공"),

    /** 소셜 계정 연동 생성 성공 */
    USER_SOCIAL_CREATE_SUCCESS(201, 3004, "사용자 소셜 계정 연동 성공"),

    /** 사용자 단건 조회 성공 */
    USER_GET_SUCCESS(200, 3005, "사용자 조회 성공"),

    /** 이메일 기준 사용자 조회 성공 */
    USER_GET_BY_EMAIL_SUCCESS(200, 3006, "이메일 기준 사용자 조회 성공"),

    /** 소셜 계정 기준 사용자 조회 성공 */
    USER_GET_BY_SOCIAL_SUCCESS(200, 3007, "소셜 계정 기준 사용자 조회 성공");

    private final int httpStatus;
    private final int code;
    private final String message;

    /**
     * 생성자
     *
     * @param httpStatus HTTP 상태 코드
     * @param code 비즈니스 성공 코드
     * @param message 성공 메시지
     */
    SuccessCode(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() { return httpStatus; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
}
