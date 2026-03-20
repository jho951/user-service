package com.core.exception;

/**
 * 서비스 전반에서 사용하는 표준 에러 코드를 정의합니다.
 */
public enum ErrorCode {

	// 공통 에러
	BAD_REQUEST(400, 7000, "잘못된 요청입니다."),
	VALIDATION_ERROR(400, 7001, "요청 필드 유효성 검사에 실패했습니다."),
	METHOD_NOT_ALLOWED(405, 7002, "허용되지 않은 HTTP 메서드입니다."),
	UNAUTHORIZED(401, 7003, "인증이 필요합니다."),
	FORBIDDEN(403, 7004, "접근 권한이 없습니다."),
	NOT_FOUND(404, 7005, "리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR(500, 7099, "서버 오류가 발생했습니다."),

	// 사용자 도메인 에러
	USER_NOT_FOUND(404, 7100, "사용자를 찾을 수 없습니다."),
	EMAIL_ALREADY_EXISTS(400, 7101, "이미 사용 중인 이메일입니다."),
	SOCIAL_ACCOUNT_ALREADY_EXISTS(400, 7102, "이미 연결된 소셜 계정입니다."),

	// 보안 / JWT 설정 에러
	NOT_FOUND_SECRET(400, 7200, "비밀키가 비어있습니다."),
	INVALID_SECRET(401, 7201, "최소 32바이트 이상의 문자열이여야합니다."),
	INVALID_TOKEN(401, 7202, "유효하지 않은 토큰입니다.");

	private final int httpStatus;
	private final int code;
	private final String message;

	/**
	 * 생성자
	 *
	 * @param httpStatus HTTP status code 숫자
	 * @param code 비즈니스 에러 코드
	 * @param message 기본 에러 메시지
	 */
	ErrorCode(int httpStatus, int code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	/**
	 * HTTP 상태 코드를 반환합니다.
	 *
	 * @return HTTP 상태 코드
	 */
	public int getHttpStatus() {return httpStatus;}

	/**
	 * 비즈니스 에러 코드를 반환합니다.
	 *
	 * @return 비즈니스 에러 코드
	 */
	public int getCode() {
		return code;
	}

	/**
	 * 에러 메시지를 반환합니다.
	 *
	 * @return 에러 메시지
	 */
	public String getMessage() {
		return message;
	}
}
