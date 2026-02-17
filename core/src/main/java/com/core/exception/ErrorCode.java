package com.core.exception;

/**
 * 공통 에러 코드 정의
 * - status: HTTP status code 숫자 (Spring 의존 X)
 * - code: 비즈니스 에러 코드 문자열
 * - message: 기본 에러 메시지
 */
public enum ErrorCode {

	// 공통 에러
	BAD_REQUEST( "C400",400, "잘못된 요청입니다."),
	UNAUTHORIZED( "C401",401, "인증이 필요합니다."),
	FORBIDDEN( "C403",403, "접근 권한이 없습니다."),
	NOT_FOUND( "C404",404, "리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR( "C500",500, "서버 오류가 발생했습니다."),

	// 사용자 관련 에러 예시
	USER_NOT_FOUND("U404", 404, "사용자를 찾을 수 없습니다."),
	EMAIL_ALREADY_EXISTS("U400",400,  "이미 사용 중인 이메일입니다."),
	INVALID_LOGIN( "U401",400, "이메일 또는 비밀번호가 올바르지 않습니다."),
	USER_DISABLED("U402",400,"허용되지 않는 유저입니다."),
	INVALID_CREDENTIALS("U403",400,"권한이 없습니다."),

	// 토근 에러
	NOT_FOUND_SECRET("T400",400, "비밀키가 비어있습니다."),
	INVALID_SECRET("T401",401, "최소 32바이트 이상의 문자열이여야합니다."),
	INVALID_TOKEN("T400",400, "유효하지 않은 리프레시 토큰입니다.");



	private final String code;
	private final int status;
	private final String message;

	ErrorCode( String code,int status, String message) {
		this.code = code;
		this.status = status;
		this.message = message;
	}

	/**
	 * HTTP 상태 코드를 반환합니다.
	 *
	 * @return HTTP 상태 코드
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * 비즈니스 에러 코드를 반환합니다.
	 *
	 * @return 에러 코드 문자열
	 */
	public String getCode() {
		return code;
	}

	/**
	 * 기본 에러 메시지를 반환합니다.
	 *
	 * @return 에러 메시지
	 */
	public String getMessage() {
		return message;
	}
}
