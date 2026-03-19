package com.api.user.dto;

import com.api.user.constant.UserSocialType;
import com.api.user.constant.UserStatus;
import com.core.constant.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequest {

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserSignupRequest {

		@Email
		@NotBlank
		private String email;

		@NotBlank
		private String name;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserCreateRequest {

		@Email
		@NotBlank
		private String email;

		@NotBlank
		private String name;

		private UserRole role;

		private UserStatus status;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserSocialCreateRequest {

		@NotNull
		private Long userId;

		@NotNull
		private UserSocialType socialType;

		@NotBlank
		private String providerId;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserStatusUpdateRequest {

		@NotNull
		private UserStatus status;
	}
}
