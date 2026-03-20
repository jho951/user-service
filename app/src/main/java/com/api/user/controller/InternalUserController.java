package com.api.user.controller;

import java.util.UUID;

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

import com.api.common.code.SuccessCode;
import com.api.common.dto.GlobalResponse;
import com.api.user.constant.UserSocialType;
import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;
import com.api.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 내부 시스템 간 사용자 연동 API를 제공합니다.
 */
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

	/**
	 * 내부 사용자 API 컨트롤러를 생성합니다.
	 *
	 * @param userService 사용자 서비스
	 */
	public InternalUserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 내부 API를 통해 사용자를 생성합니다.
	 *
	 * @param request 사용자 생성 요청
	 * @return 생성된 사용자 응답
	 */
	@PostMapping
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> createUser(
		@Valid @RequestBody UserRequest.UserCreateRequest request
	) {
		SuccessCode successCode = SuccessCode.USER_CREATE_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.create(request)));
	}

	/**
	 * 사용자 소셜 계정 연동 정보를 생성합니다.
	 *
	 * @param request 소셜 계정 생성 요청
	 * @return 생성된 소셜 계정 응답
	 */
	@PostMapping("/social")
	public ResponseEntity<GlobalResponse<UserResponse.UserSocialResponse>> createSocial(
		@Valid @RequestBody UserRequest.UserSocialCreateRequest request
	) {
		SuccessCode successCode = SuccessCode.USER_SOCIAL_CREATE_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.createSocial(request)));
	}

	/**
	 * 사용자 상태를 변경합니다.
	 *
	 * @param userId 사용자 식별자
	 * @param request 상태 변경 요청
	 * @return 변경된 사용자 응답
	 */
	@PatchMapping("/{userId}/status")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> updateStatus(
		@PathVariable UUID userId,
		@Valid @RequestBody UserRequest.UserStatusUpdateRequest request
	) {
		SuccessCode successCode = SuccessCode.USER_STATUS_UPDATE_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.updateStatus(userId, request)));
	}

	/**
	 * 사용자 식별자로 사용자를 조회합니다.
	 *
	 * @param userId 사용자 식별자
	 * @return 사용자 응답
	 */
	@GetMapping("/{userId}")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> getUser(@PathVariable UUID userId) {
		SuccessCode successCode = SuccessCode.USER_GET_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.get(userId)));
	}

	/**
	 * 이메일로 사용자를 조회합니다.
	 *
	 * @param email 사용자 이메일
	 * @return 사용자 응답
	 */
	@GetMapping("/by-email")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> getUserByEmail(@RequestParam String email) {
		SuccessCode successCode = SuccessCode.USER_GET_BY_EMAIL_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.getByEmail(email)));
	}

	/**
	 * 소셜 제공자 정보로 사용자를 조회합니다.
	 *
	 * @param socialType 소셜 제공자 타입
	 * @param providerId 제공자 사용자 식별값
	 * @return 사용자 응답
	 */
	@GetMapping("/by-social")
	public ResponseEntity<GlobalResponse<UserResponse.UserDetailResponse>> getUserBySocial(
		@RequestParam UserSocialType socialType,
		@RequestParam String providerId
	) {
		SuccessCode successCode = SuccessCode.USER_GET_BY_SOCIAL_SUCCESS;
		return ResponseEntity
			.status(successCode.getHttpStatus())
			.body(GlobalResponse.ok(successCode, userService.getBySocial(socialType, providerId)));
	}
}
