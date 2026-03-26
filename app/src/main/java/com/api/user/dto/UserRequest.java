package com.api.user.dto;

import java.util.UUID;

import com.api.user.constant.UserSocialType;
import com.api.user.constant.UserStatus;
import com.core.constant.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 API 요청 DTO를 모아둔 클래스입니다.
 */
public class UserRequest {

	/**
	 * 공개 회원가입 요청 DTO입니다.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserSignupRequest {

		@Email
		@NotBlank
		private String email;
	}

	/**
	 * 내부 사용자 생성 요청 DTO입니다.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserCreateRequest {

		@Email
		@NotBlank
		private String email;

		private UserRole role;

		private UserStatus status;
	}

	/**
	 * 소셜 계정 생성 요청 DTO입니다.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserSocialCreateRequest {

		@NotNull
		private UUID userId;

		@NotNull
		private UserSocialType socialType;

		@NotBlank
		private String providerId;
	}

	/**
	 * 소셜 사용자 보장 요청 DTO입니다.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserEnsureSocialRequest {

		@Email
		@NotBlank
		private String email;

		@NotNull
		private UserSocialType socialType;

		@NotBlank
		private String providerId;

		private UserRole role;

		private UserStatus status;
	}

	/**
	 * 사용자 상태 변경 요청 DTO입니다.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserStatusUpdateRequest {

		@NotNull
		private UserStatus status;
	}
}
