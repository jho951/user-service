package com.api.user.dto;

import com.core.constant.UserRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 관리자 사용자 생성/수정 요청 DTO입니다.
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRequestDto {

	/** 사용자 로그인 아이디 */
	private String username;
	/** 원문 비밀번호(저장 시 암호화됨) */
	private String password;
	/** 사용자 이메일 */
	private String email;
	/** 사용자 권한 */
	private UserRole role;
	/** 계정 활성화 여부 */
	private Boolean enabled;
}
