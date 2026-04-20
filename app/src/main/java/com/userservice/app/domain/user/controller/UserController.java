package com.userservice.app.domain.user.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.userservice.app.common.base.constant.SuccessCode;
import com.userservice.app.common.base.dto.GlobalResponse;
import com.userservice.app.domain.user.dto.UserRequest;
import com.userservice.app.domain.user.dto.UserResponse;
import com.userservice.app.domain.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 공개 사용자 API를 제공합니다.
 */
@RestController
@RequestMapping("/users")
@ConditionalOnProperty(prefix = "features.public-user-api", name = "enabled", havingValue = "true")
public class UserController {

	private final UserService userService;

	/**
	 * 공개 사용자 API 컨트롤러를 생성합니다.
	 *
	 * @param userService 사용자 서비스
	 */
	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 인증된 사용자의 내 정보를 조회합니다.
	 *
	 * @param authentication 인증 객체
	 * @return 내 사용자 정보 응답
	 */
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> me(Authentication authentication) {
		UUID userId = resolveUserId(authentication);
		return GlobalResponse.success(SuccessCode.USER_ME_GET_SUCCESS, userService.getMe(userId));
	}

	private UUID resolveUserId(Authentication authentication) {
		String name = authentication.getName();
		if (name != null && !name.isBlank()) {
			return UUID.fromString(name);
		}

		throw new IllegalStateException("Unsupported user principal type");
	}

	/**
	 * 공개 회원가입을 처리합니다.
	 *
	 * @param request 회원가입 요청
	 * @return 생성된 사용자 응답
	 */
	@PostMapping("/signup")
	public ResponseEntity<GlobalResponse<UserResponse.UserCreateResponse>> signup(
		@Valid @RequestBody UserRequest.UserSignupRequest request
	) {
		return GlobalResponse.success(SuccessCode.USER_SIGNUP_SUCCESS, userService.signup(request));
	}
}
