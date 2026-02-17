package com.api.config;

import com.core.dto.BaseResponse;

import com.core.exception.ErrorCode;
import com.core.exception.BusinessException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 전역 예외를 공통 응답 형식으로 변환하는 핸들러입니다.
 */
@RestControllerAdvice(basePackages = "com.api")
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 예외를 처리합니다.
	 *
	 * @param e 비즈니스 예외
	 * @return 에러 응답
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(BaseResponse.error(errorCode, e.getMessage()));
	}

	/**
	 * 처리되지 않은 예외를 처리합니다.
	 *
	 * @param e 예외
	 * @return 내부 서버 오류 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(BaseResponse.error(errorCode));
	}
}
