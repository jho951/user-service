package com.api.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.user.constant.UserSocialType;
import com.api.user.constant.UserStatus;
import com.api.user.dto.UserRequest;
import com.api.user.dto.UserResponse;
import com.api.user.entity.User;
import com.api.user.entity.UserSocial;
import com.api.user.repository.UserRepository;
import com.api.user.repository.UserSocialRepository;
import com.core.constant.UserRole;
import com.core.exception.BusinessException;
import com.core.exception.ErrorCode;

/**
 * 사용자 유즈케이스 구현체입니다.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserSocialRepository userSocialRepository;

	/**
	 * 사용자 서비스 구현체를 생성합니다.
	 *
	 * @param userRepository 사용자 저장소
	 * @param userSocialRepository 사용자 소셜 계정 저장소
	 */
	public UserServiceImpl(UserRepository userRepository, UserSocialRepository userSocialRepository) {
		this.userRepository = userRepository;
		this.userSocialRepository = userSocialRepository;
	}

	@Override
	/**
	 * 공개 회원가입을 처리합니다.
	 *
	 * @param request 회원가입 요청
	 * @return 생성된 사용자 응답
	 */
	public UserResponse.UserCreateResponse signup(UserRequest.UserSignupRequest request) {
		User savedUser = saveUser(request.getEmail(), UserRole.USER, UserStatus.ACTIVE);
		return UserResponse.UserCreateResponse.from(savedUser);
	}

	@Override
	/**
	 * 내부 API를 통해 사용자를 생성합니다.
	 *
	 * @param request 사용자 생성 요청
	 * @return 생성된 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse create(UserRequest.UserCreateRequest request) {
		User savedUser = saveUser(
			request.getEmail(),
			request.getRole() != null ? request.getRole() : UserRole.USER,
			request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE
		);
		return UserResponse.UserDetailResponse.from(savedUser);
	}

	@Override
	/**
	 * 사용자 소셜 계정 연동 정보를 생성합니다.
	 *
	 * @param request 소셜 계정 생성 요청
	 * @return 생성된 소셜 계정 응답
	 */
	public UserResponse.UserSocialResponse createSocial(UserRequest.UserSocialCreateRequest request) {
		if (userSocialRepository.existsBySocialTypeAndProviderId(request.getSocialType(), request.getProviderId())) {
			throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_EXISTS);
		}

		User user = getUserEntity(request.getUserId());
		UserSocial userSocial = UserSocial.builder()
			.user(user)
			.socialType(request.getSocialType())
			.providerId(request.getProviderId())
			.build();

		UserSocial savedUserSocial = userSocialRepository.save(userSocial);
		user.addUserSocial(savedUserSocial);

		return UserResponse.UserSocialResponse.from(savedUserSocial);
	}

	@Override
	/**
	 * 사용자 상태를 변경합니다.
	 *
	 * @param userId 사용자 식별자
	 * @param request 상태 변경 요청
	 * @return 변경된 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse updateStatus(UUID userId, UserRequest.UserStatusUpdateRequest request) {
		User user = getUserEntity(userId);
		user.changeStatus(request.getStatus());
		return UserResponse.UserDetailResponse.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	/**
	 * 사용자 식별자로 사용자를 조회합니다.
	 *
	 * @param userId 사용자 식별자
	 * @return 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse get(UUID userId) {
		return UserResponse.UserDetailResponse.from(getUserEntityWithSocials(userId));
	}

	@Override
	@Transactional(readOnly = true)
	/**
	 * 이메일로 사용자를 조회합니다.
	 *
	 * @param email 사용자 이메일
	 * @return 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse getByEmail(String email) {
		User user = userRepository.findWithUserSocialListByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return UserResponse.UserDetailResponse.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	/**
	 * 소셜 제공자 정보로 사용자를 조회합니다.
	 *
	 * @param socialType 소셜 제공자 타입
	 * @param providerId 제공자 사용자 식별값
	 * @return 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse getBySocial(UserSocialType socialType, String providerId) {
		UserSocial userSocial = userSocialRepository.findBySocialTypeAndProviderId(socialType, providerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return get(userSocial.getUser().getId());
	}

	/**
	 * 이메일 중복을 확인한 뒤 사용자를 저장합니다.
	 *
	 * @param email 사용자 이메일
	 * @param role 사용자 권한
	 * @param status 사용자 상태
	 * @return 저장된 사용자 엔티티
	 */
	private User saveUser(String email, UserRole role, UserStatus status) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		User user = User.builder()
			.email(email)
			.role(role)
			.status(status)
			.build();

		return userRepository.save(user);
	}

	/**
	 * 사용자 식별자로 엔티티를 조회합니다.
	 *
	 * @param userId 사용자 식별자
	 * @return 조회된 사용자 엔티티
	 */
	private User getUserEntity(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 소셜 계정 목록을 함께 로딩하여 사용자 엔티티를 조회합니다.
	 *
	 * @param userId 사용자 식별자
	 * @return 소셜 계정이 포함된 사용자 엔티티
	 */
	private User getUserEntityWithSocials(UUID userId) {
		return userRepository.findWithUserSocialListById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
