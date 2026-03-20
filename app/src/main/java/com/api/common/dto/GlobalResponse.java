package com.api.common.dto;

import com.api.common.code.SuccessCode;
import com.core.exception.ErrorCode;

public final class GlobalResponse<T> {
    private final int httpStatus;
    private final boolean success;
    private final String message;
    private final int code;
    private final T data;

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

    public static GlobalResponse<Void> ok() {
        return ok(SuccessCode.GET_SUCCESS, null);
    }

    public static GlobalResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

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
