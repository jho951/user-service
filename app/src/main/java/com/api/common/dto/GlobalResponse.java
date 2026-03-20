package com.api.common.dto;

import com.api.common.code.SuccessCode;
import com.core.exception.ErrorCode;

/**
 * API 응답 바디를 공통 형식으로 감싸는 래퍼입니다.
 *
 * @param <T> 응답 데이터 타입
 */
public final class GlobalResponse<T> {
    private final int httpStatus;
    private final boolean success;
    private final String message;
    private final int code;
    private final T data;

    /**
     * 공통 응답 객체를 생성합니다.
     *
     * @param httpStatus HTTP 상태 코드
     * @param success 성공 여부
     * @param message 응답 메시지
     * @param code 비즈니스 코드
     * @param data 실제 응답 데이터
     */
    public GlobalResponse(int httpStatus, boolean success, String message, int code, T data) {
        if (message == null) {
            throw new IllegalArgumentException("메시지는 null일 수 없습니다.");
        }
        this.httpStatus = httpStatus;
        this.success = success;
        this.message = message;
        this.code = code;
        this.data = data;
    }

    /**
     * 성공 응답을 생성합니다.
     *
     * @param successCode 성공 메타정보
     * @param data 실제 응답 데이터
     * @param <T> 응답 데이터 타입
     * @return 성공 응답 객체
     */
    public static <T> GlobalResponse<T> ok(SuccessCode successCode, T data) {
        if (successCode == null) {
            throw new IllegalArgumentException("성공 코드는 null일 수 없습니다.");
        }
        return new GlobalResponse<>(
                successCode.getHttpStatus(),
                true,
                successCode.getMessage(),
                successCode.getCode(),
                data
        );
    }

    /**
     * 데이터 없는 기본 성공 응답을 생성합니다.
     *
     * @return 기본 성공 응답
     */
    public static GlobalResponse<Void> ok() {
        return ok(SuccessCode.USER_GET_SUCCESS, null);
    }

    /**
     * 기본 메시지로 실패 응답을 생성합니다.
     *
     * @param errorCode 에러 메타정보
     * @return 실패 응답 객체
     */
    public static GlobalResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    /**
     * 커스텀 메시지로 실패 응답을 생성합니다.
     *
     * @param errorCode 에러 메타정보
     * @param message 응답 메시지
     * @return 실패 응답 객체
     */
    public static GlobalResponse<Void> fail(ErrorCode errorCode, String message) {
        if (errorCode == null) {
            throw new IllegalArgumentException("에러 코드는 null일 수 없습니다.");
        }
        return new GlobalResponse<>(
                errorCode.getHttpStatus(),
                false,
                message,
                errorCode.getCode(),
                null
        );
    }

    public int getHttpStatus() {
        return httpStatus;
    }
    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public int getCode() {
        return code;
    }
    public T getData() {
        return data;
    }
}
