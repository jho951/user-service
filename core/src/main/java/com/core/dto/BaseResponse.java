package com.core.dto;

import com.core.exception.ErrorCode;

import lombok.Getter;
import lombok.Builder;

/**
 * API 공통 응답 래퍼
 * @param <T> data 페이로드 타입
 */
@Getter
public class BaseResponse<T> {

	private final boolean success;
	private final String code;
	private final String message;
	private final T data;

	/**
	 * 응답 구조 생성자
	 *
	 * @param success 요청 성공 여부
	 * @param code  비즈니스 에러 코드 (성공 시 "OK" 등)
	 * @param message 사용자에게 보여줄 메시지
	 * @param data 실제 응답 데이터
	 */
	@Builder
	private BaseResponse(boolean success, String code, String message, T data) {
		this.success = success;
		this.code = code;
		this.message = message;
		this.data = data;
	}


	/** 성공 응답 (기본 메시지 "성공") */
	public static <T> BaseResponse<T> ok(T data) {
		return BaseResponse.<T>builder()
			.success(true)
			.code("OK")
			.message("성공")
			.data(data)
			.build();
	}

	/** 메시지 커스텀 가능한 성공 응답 */
	public static <T> BaseResponse<T> ok(T data, String message) {
		return BaseResponse.<T>builder()
			.success(true)
			.code("OK")
			.message(message)
			.data(data)
			.build();
	}

	/** 에러 응답 (ErrorCode 기본 메시지 사용) */
	public static <T> BaseResponse<T> error(ErrorCode errorCode) {
		return BaseResponse.<T>builder()
			.success(false)
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.data(null)
			.build();
	}

	/** 에러 응답 (메시지 오버라이드) */
	public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
		return BaseResponse.<T>builder()
			.success(false)
			.code(errorCode.getCode())
			.message(message)
			.data(null)
			.build();
	}
}
