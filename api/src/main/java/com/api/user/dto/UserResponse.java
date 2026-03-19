package com.api.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.api.user.constant.UserSocialType;
import com.api.user.constant.UserStatus;
import com.api.user.entity.User;
import com.api.user.entity.UserSocial;
import com.core.constant.UserRole;

import lombok.Builder;
import lombok.Getter;

public class UserResponse {

	@Getter
	@Builder
	public static class UserSocialResponse {
		private Long id;
		private UserSocialType socialType;
		private String providerId;

		public static UserSocialResponse from(UserSocial userSocial) {
			return UserSocialResponse.builder()
				.id(userSocial.getId())
				.socialType(userSocial.getSocialType())
				.providerId(userSocial.getProviderId())
				.build();
		}
	}

	@Getter
	@Builder
	public static class UserDetailResponse {
		private Long id;
		private String email;
		private String name;
		private UserRole role;
		private UserStatus status;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private List<UserSocialResponse> userSocialList;

		public static UserDetailResponse from(User user) {
			return UserDetailResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.name(user.getName())
				.role(user.getRole())
				.status(user.getStatus())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.userSocialList(user.getUserSocialList().stream()
					.map(UserSocialResponse::from)
					.toList())
				.build();
		}
	}

	@Getter
	@Builder
	public static class UserCreateResponse {
		private UserDetailResponse user;

		public static UserCreateResponse from(User user) {
			return UserCreateResponse.builder()
				.user(UserDetailResponse.from(user))
				.build();
		}
	}
}
