package com.api.user.service;

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

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserSocialRepository userSocialRepository;

	public UserServiceImpl(UserRepository userRepository, UserSocialRepository userSocialRepository) {
		this.userRepository = userRepository;
		this.userSocialRepository = userSocialRepository;
	}

	@Override
	public UserResponse.UserCreateResponse signup(UserRequest.UserSignupRequest request) {
		User savedUser = saveUser(request.getEmail(), request.getName(), UserRole.USER, UserStatus.ACTIVE);
		return UserResponse.UserCreateResponse.from(savedUser);
	}

	@Override
	public UserResponse.UserDetailResponse create(UserRequest.UserCreateRequest request) {
		User savedUser = saveUser(
			request.getEmail(),
			request.getName(),
			request.getRole() != null ? request.getRole() : UserRole.USER,
			request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE
		);
		return UserResponse.UserDetailResponse.from(savedUser);
	}

	@Override
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
	public UserResponse.UserDetailResponse updateStatus(Long userId, UserRequest.UserStatusUpdateRequest request) {
		User user = getUserEntity(userId);
		user.changeStatus(request.getStatus());
		return UserResponse.UserDetailResponse.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse.UserDetailResponse get(Long userId) {
		return UserResponse.UserDetailResponse.from(getUserEntityWithSocials(userId));
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse.UserDetailResponse getByEmail(String email) {
		User user = userRepository.findWithUserSocialListByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return UserResponse.UserDetailResponse.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse.UserDetailResponse getBySocial(UserSocialType socialType, String providerId) {
		UserSocial userSocial = userSocialRepository.findBySocialTypeAndProviderId(socialType, providerId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return get(userSocial.getUser().getId());
	}

	private User saveUser(String email, String name, UserRole role, UserStatus status) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		User user = User.builder()
			.email(email)
			.name(name)
			.role(role)
			.status(status)
			.build();

		return userRepository.save(user);
	}

	private User getUserEntity(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private User getUserEntityWithSocials(Long userId) {
		return userRepository.findWithUserSocialListById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
