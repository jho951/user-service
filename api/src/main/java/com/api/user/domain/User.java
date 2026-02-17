package com.api.user.domain;

import com.core.constant.UserRole;
import com.core.domain.BaseEntity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 관리자 사용자 엔티티입니다.
 */
@Entity
@Table(name = "admin_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Column(nullable = false)
	private Boolean enabled;

	@Builder
	private User(
		String username,
		String password,
		String email,
		UserRole role,
		Boolean enabled
	) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.role = role;
		this.enabled = enabled;
	}

	/**
	 * 암호화된 비밀번호를 변경합니다.
	 *
	 * @param encodedPassword 암호화된 비밀번호
	 */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	/**
	 * 역할을 변경합니다.
	 *
	 * @param role 변경할 역할
	 */
	public void changeRole(UserRole role) {
		this.role = role;
	}

	/**
	 * 이메일을 변경합니다.
	 *
	 * @param email 변경할 이메일
	 */
	public void changeEmail(String email) {
		this.email = email;
	}

	/**
	 * 활성화 여부를 변경합니다.
	 *
	 * @param enabled 활성화 여부
	 */
	public void changeEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * 계정을 활성화합니다.
	 */
	public void enable() {
		this.enabled = true;
	}

	/**
	 * 계정을 비활성화합니다.
	 */
	public void disable() {
		this.enabled = false;
	}
}
