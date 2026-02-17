package com.core.constant;

/**
 * 애플리케이션에서 사용하는 사용자 권한(Role) 상수(enum)입니다.
 *
 * <p>Spring Security의 권한 체계와 호환되도록 각 Role은 {@code ROLE_} 접두어가 포함된
 * authority 문자열을 가집니다.</p>
 */
public enum UserRole {

	/** 최고 관리자 권한 (authority: {@code ROLE_SUPER_ADMIN}) */
	SUPER_ADMIN("ROLE_SUPER_ADMIN"),

	/** 관리자 권한 (authority: {@code ROLE_ADMIN}) */
	ADMIN("ROLE_ADMIN"),

	/** 매니저 권한 (authority: {@code ROLE_MANAGER}) */
	MANAGER("ROLE_MANAGER"),

	/** 일반 사용자 권한 (authority: {@code ROLE_USER}) */
	USER("ROLE_USER");

	/**
	 * Spring Security에서 사용하는 권한 문자열(authority) 값입니다.
	 * <p>일반적으로 {@code ROLE_} 접두어를 포함합니다.</p>
	 */
	private final String authority;

	/**
	 * 지정된 authority 문자열로 {@link UserRole}을 생성합니다.
	 *
	 * @param authority 권한 문자열 (예: {@code ROLE_ADMIN})
	 */
	UserRole(String authority) {
		this.authority = authority;
	}

	/**
	 * Spring Security에서 비교/검증에 사용하는 권한 문자열(authority)을 반환합니다.
	 *
	 * @return 권한 문자열 (예: {@code ROLE_USER})
	 */
	public String authority() {
		return authority;
	}
}
