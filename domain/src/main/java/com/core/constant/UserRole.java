package com.core.constant;

import java.util.Arrays;

/**
 * 사용자 권한입니다.
 * <p>
 * Spring Security의 권한 체계와 호환되도록
 * 각 Role은 {@code ROLE_} 접두어가 포함된 authority 문자열을 가집니다.
 * </p>
 */
public enum UserRole {

	/** 시스템 전체 관리자 역할*/
	SUPER_ADMIN("시스템 전체 관리자", "ROLE_SUPER_ADMIN"),
	/** 워크스페이스 관리자 역할 */
	ADMIN("워크스페이스(조직) 관리자", "ROLE_ADMIN"),
	/** 일반 사용자 역할*/
	USER("일반 사용자", "ROLE_USER"),
	/** 외부 협업자 역할 */
	GUEST("외부 협업자", "ROLE_GUEST");

	private final String description;
	private final String authority;

	/**
	 * 생성자
	 * @param description 역할 설명
	 * @param authority Spring Security에서 비교/검증에 사용하는 권한 문자열(authority)
	 */
	UserRole(String description, String authority) {
		this.description = description;
		this.authority = authority;
	}

	public String description() {return description;}
	public String authority() {return authority;}

	/**
	 * enum 상수 또는 authority 문자열을 {@link UserRole}로 변환합니다.
	 * <p>
	 * {@code ADMIN}과 {@code ROLE_ADMIN} 모두 허용합니다.
	 * </p>
	 * @param value role 또는 authority 문자열
	 * @return 매핑된 사용자 권한
	 * @throws IllegalArgumentException 매핑되는 권한이 없을 때
	 */
	public static UserRole from(String value) {
		return Arrays.stream(values())
			.filter(role -> role.name().equals(value) || role.authority.equals(value))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown user role: " + value));
	}
}
