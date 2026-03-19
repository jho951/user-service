package com.api.user.controller;

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

import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;
import com.api.user.service.UserService;
import com.core.dto.BaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@ConditionalOnProperty(prefix = "features.public-user-api", name = "enabled", havingValue = "true")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PreAuthorize("@jwtAccessPolicy.hasActiveStatus(authentication)")
	@GetMapping("/me")
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> me(@AuthenticationPrincipal Jwt jwt) {
		Long userId = Long.valueOf(jwt.getSubject());
		return ResponseEntity.ok(BaseResponse.ok(userService.get(userId)));
	}

	@PostMapping("/signup")
	public ResponseEntity<BaseResponse<UserResponse.UserCreateResponse>> signup(
		@Valid @RequestBody UserRequest.UserSignupRequest request
	) {
		return ResponseEntity.ok(BaseResponse.ok(userService.signup(request), "회원이 생성되었습니다."));
	}
}
