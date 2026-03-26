package com.api.user.service;

import java.util.UUID;

import com.api.user.constant.UserSocialType;
import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;

/**
 * 사용자 도메인 유즈케이스를 정의합니다.
 */
public interface UserService {

	/**
	 * 공개 회원가입을 처리합니다.
	 *
	 * @param request 회원가입 요청
	 * @return 생성된 사용자 응답
	 */
	UserResponse.UserCreateResponse signup(UserRequest.UserSignupRequest request);

	/**
	 * 내부 API를 통해 사용자를 생성합니다.
	 *
	 * @param request 사용자 생성 요청
	 * @return 생성된 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse create(UserRequest.UserCreateRequest request);

	/**
	 * 사용자 소셜 계정 연동 정보를 생성합니다.
	 *
	 * @param request 소셜 계정 생성 요청
	 * @return 생성된 소셜 계정 응답
	 */
	UserResponse.UserSocialResponse createSocial(UserRequest.UserSocialCreateRequest request);

	/**
	 * 소셜 사용자 보장 유즈케이스를 처리합니다.
	 *
	 * @param request 소셜 사용자 보장 요청
	 * @return 보장된 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse ensureSocial(UserRequest.UserEnsureSocialRequest request);

	/**
	 * 사용자 상태를 변경합니다.
	 *
	 * @param userId 사용자 식별자
	 * @param request 상태 변경 요청
	 * @return 변경된 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse updateStatus(UUID userId, UserRequest.UserStatusUpdateRequest request);

	/**
	 * 사용자 식별자로 사용자를 조회합니다.
	 *
	 * @param userId 사용자 식별자
	 * @return 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse get(UUID userId);

	/**
	 * 이메일로 사용자를 조회합니다.
	 *
	 * @param email 사용자 이메일
	 * @return 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse getByEmail(String email);

	/**
	 * 소셜 제공자와 제공자 식별값으로 사용자를 조회합니다.
	 *
	 * @param socialType 소셜 제공자 타입
	 * @param providerId 소셜 제공자 사용자 식별값
	 * @return 사용자 상세 응답
	 */
	UserResponse.UserDetailResponse getBySocial(UserSocialType socialType, String providerId);
}
