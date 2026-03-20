package com.api.user.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.common.code.SuccessCode;
import com.api.common.dto.GlobalResponse;
import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;
import com.api.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 공개 사용자 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/users")
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
	 * @param jwt 인증된 JWT
	 * @return 내 사용자 정보 응답
	 */
	@PreAuthorize("@jwtAccessPolicy.hasActiveStatus(authentication)")
	@GetMapping("/me")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> me(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		SuccessCode successCode = SuccessCode.USER_ME_GET_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.get(userId)));
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
		SuccessCode successCode = SuccessCode.USER_SIGNUP_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.signup(request)));
	}
}
