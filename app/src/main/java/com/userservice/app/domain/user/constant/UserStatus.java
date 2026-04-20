package com.userservice.app.domain.user.constant;

import java.util.Arrays;

/**
 * 사용자 상태를 정의합니다.
 */
public enum UserStatus {
	ACTIVE("정상", "A"),
	PENDING("승인대기", "P"),
	SUSPENDED("정지", "S"),
	DELETED("탈퇴", "D");

	private final String description;
	private final String code;

	/**
	 * 생성자
	 * @param description 상태 설명
	 * @param code DB에 저장되는 상태 코드
	 */
	UserStatus(String description, String code) {
		this.description = description;
		this.code = code;
	}

	/**
	 * 상태 설명을 반환합니다.
	 *
	 * @return 상태 설명
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * DB 저장 코드를 반환합니다.
	 *
	 * @return 상태 코드
	 */
	public String getCode() {
		return code;
	}

	/**
	 * DB 코드를 enum으로 변환합니다.
	 *
	 * @param code DB 저장 상태 코드
	 * @return 매핑된 사용자 상태
	 */
	public static UserStatus fromCode(String code) {
		return Arrays.stream(values())
			.filter(status -> status.code.equals(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown user status code: " + code));
	}
}
