package com.api.user.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.user.constant.UserSocialType;
import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;
import com.api.user.service.UserService;
import com.core.dto.BaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/users")
@ConditionalOnProperty(
	prefix = "features.internal-user-api",
	name = "enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class InternalUserController {

	private final UserService userService;

	public InternalUserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> createUser(
		@Valid @RequestBody UserRequest.UserCreateRequest request
	) {
		return ResponseEntity.ok(BaseResponse.ok(userService.create(request)));
	}

	@PostMapping("/social")
	public ResponseEntity<BaseResponse<UserResponse.UserSocialResponse>> createSocial(
		@Valid @RequestBody UserRequest.UserSocialCreateRequest request
	) {
		return ResponseEntity.ok(BaseResponse.ok(userService.createSocial(request)));
	}

	@PatchMapping("/{userId}/status")
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> updateStatus(
		@PathVariable Long userId,
		@Valid @RequestBody UserRequest.UserStatusUpdateRequest request
	) {
		return ResponseEntity.ok(BaseResponse.ok(userService.updateStatus(userId, request)));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> getUser(@PathVariable Long userId) {
		return ResponseEntity.ok(BaseResponse.ok(userService.get(userId)));
	}

	@GetMapping("/by-email")
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> getUserByEmail(@RequestParam String email) {
		return ResponseEntity.ok(BaseResponse.ok(userService.getByEmail(email)));
	}

	@GetMapping("/by-social")
	public ResponseEntity<BaseResponse<UserResponse.UserDetailResponse>> getUserBySocial(
		@RequestParam UserSocialType socialType,
		@RequestParam String providerId
	) {
		return ResponseEntity.ok(BaseResponse.ok(userService.getBySocial(socialType, providerId)));
	}
}
