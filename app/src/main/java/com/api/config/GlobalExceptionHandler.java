package com.api.config;

import com.api.common.dto.GlobalResponse;
import com.core.exception.ErrorCode;
import com.core.exception.BusinessException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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
	public ResponseEntity<GlobalResponse<Void>> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode, e.getMessage()));
	}

	/**
	 * 검증 예외를 처리합니다.
	 *
	 * @param e 검증 예외
	 * @return 에러 응답
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<GlobalResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
		String message = e.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
			.orElse(errorCode.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode, message));
	}

	/**
	 * 허용되지 않은 HTTP 메서드 예외를 처리합니다.
	 *
	 * @param e HTTP 메서드 예외
	 * @return 에러 응답
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<GlobalResponse<Void>> handleHttpRequestMethodNotSupportedException(
		HttpRequestMethodNotSupportedException e
	) {
		ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode));
	}

	/**
	 * 처리되지 않은 예외를 처리합니다.
	 *
	 * @param e 예외
	 * @return 내부 서버 오류 응답
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<GlobalResponse<Void>> handleException(Exception e) {
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(GlobalResponse.fail(errorCode));
	}
}
