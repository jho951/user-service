package com.core.exception;

/**
 * 서비스 전반에서 사용하는 공통 비즈니스 예외
 * - 반드시 ErrorCode를 하나 가지도록 설계
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	/**
	 * 기본 메시지(ErrorCode의 메시지)로 예외를 생성합니다.
	 *
	 * @param errorCode 에러 코드
	 */
	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/**
	 * 커스텀 메시지로 예외를 생성합니다.
	 *
	 * @param errorCode 에러 코드
	 * @param message 사용자 정의 메시지
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * 에러 코드를 반환합니다.
	 *
	 * @return 에러 코드
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * HTTP 상태 코드를 반환합니다.
	 *
	 * @return HTTP 상태 코드
	 */
	public int getStatus() {
		return errorCode.getStatus();
	}

	/**
	 * 비즈니스 에러 코드를 반환합니다.
	 *
	 * @return 에러 코드 문자열
	 */
	public String getCode() {
		return errorCode.getCode();
	}
}
