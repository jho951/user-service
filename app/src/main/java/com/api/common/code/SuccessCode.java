package com.api.common.code;

public enum SuccessCode {
    /** 조회 성공 (GET) */
    GET_SUCCESS(200, 200, "조회 요청 성공"),

    /** 생성 성공 (POST) */
    CREATE_SUCCESS(201, 201, "리소스 생성 성공"),

    /**  수정 성공 (PUT, PATCH) */
    UPDATE_SUCCESS(200, 200, "리소스 수정 성공"),

    /**  삭제 성공 (DELETE) */
    DELETE_SUCCESS(200, 200, "리소스 삭제 성공"),

    /**  비동기 요청 접수 */
    PROCESS_ACCEPTED(202, 202, "요청이 접수 성공");

    private final int httpStatus;
    private final int code;
    private final String message;

    /**
     * 생성자
     *
     * @param httpStatus 상태
     * @param code 상태 코드
     * @param message 추가 메시지
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
