package com.api.user.dto;

import com.api.user.domain.User;
import com.core.constant.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 사용자 응답 DTO입니다.
 */
@Getter
@Builder
public class UserResponseDto {

	private Long id;
	private String username;
	private String email;
	private UserRole role;
	private Boolean enabled;

	/**
	 * 엔티티를 응답 DTO로 변환합니다.
	 *
	 * @param user 사용자 엔티티
	 * @return 사용자 응답 DTO
	 */
	public static UserResponseDto from(User user) {
		return UserResponseDto.builder()
			.id(user.getId())
			.username(user.getUsername())
			.email(user.getEmail())
			.role(user.getRole())
			.enabled(user.getEnabled())
			.build();
	}
}
