package com.userservice.app.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userservice.app.domain.user.constant.UserSocialType;
import com.userservice.app.domain.user.constant.UserStatus;
import com.userservice.app.domain.user.dto.UserRequest;
import com.userservice.app.domain.user.dto.UserResponse;
import com.userservice.app.domain.user.entity.User;
import com.userservice.app.domain.user.entity.UserSocial;
import com.userservice.app.domain.audit.UserAuditLogService;
import com.userservice.app.domain.user.observability.SocialLinkMetrics;
import com.userservice.app.domain.user.repository.UserRepository;
import com.userservice.app.domain.user.repository.UserSocialRepository;
import com.userservice.app.domain.user.constant.UserRole;
import com.userservice.app.common.base.exception.BusinessException;
import com.userservice.app.common.base.constant.ErrorCode;

/** 사용자 유즈케이스 구현체입니다. */
@Service
@Transactional
public class UserServiceImpl implements UserService {

	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	private final UserRepository userRepository;
	private final UserSocialRepository userSocialRepository;
	private final SocialLinkMetrics socialLinkMetrics;
	private final UserAuditLogService userAuditLogService;

	/**
	 * 사용자 서비스 구현체를 생성합니다.
	 *
	 * @param userRepository 사용자 저장소
	 * @param userSocialRepository 사용자 소셜 계정 저장소
	 */
	public UserServiceImpl(
		UserRepository userRepository,
		UserSocialRepository userSocialRepository,
		SocialLinkMetrics socialLinkMetrics,
		UserAuditLogService userAuditLogService
	) {
		this.userRepository = userRepository;
		this.userSocialRepository = userSocialRepository;
		this.socialLinkMetrics = socialLinkMetrics;
		this.userAuditLogService = userAuditLogService;
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
		userAuditLogService.logSignup(savedUser);
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
		userAuditLogService.logInternalCreate(savedUser);
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
		User user = getUserEntity(request.getUserId());
		LinkResult linkResult = linkSocialIdempotently(user, request.getSocialType(), request.getProviderId(), null);
		userAuditLogService.logSocialLink(linkResult.userSocial, "POST /internal/users/social");
		return UserResponse.UserSocialResponse.from(linkResult.userSocial);
	}

	@Override
	/**
	 * 소셜 사용자 보장 유즈케이스를 처리합니다.
	 *
	 * @param request 소셜 사용자 보장 요청
	 * @return 보장된 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse ensureSocial(UserRequest.UserEnsureSocialRequest request) {
		return findOrCreateAndLinkSocial(request);
	}

	@Override
	public UserResponse.UserDetailResponse findOrCreateAndLinkSocial(UserRequest.UserEnsureSocialRequest request) {
		Instant startedAt = Instant.now();
		socialLinkMetrics.incrementRequests();

		String provider = request.getSocialType().name();
		String maskedProviderUserId = hashProviderUserId(request.getProviderId());
		String email = request.getEmail();

		boolean conflict = false;
		String result = "error";
		String errorCode = null;

		try {
			UserSocial existingSocial = userSocialRepository
				.findBySocialTypeAndProviderId(request.getSocialType(), request.getProviderId())
				.orElse(null);
			if (existingSocial != null) {
				syncLinkedEmail(existingSocial, email);
				socialLinkMetrics.incrementExisting();
				result = "existing";
				userAuditLogService.logSocialLink(existingSocial, "POST /internal/users/ensure-social");
				log.info(
					"social_link provider={} providerUserId={} email={} userId={} result={} errorCode={}",
					provider,
					maskedProviderUserId,
					email,
					existingSocial.getUser().getId(),
					result,
					errorCode
				);
				return get(existingSocial.getUser().getId());
			}

			UserRole role = request.getRole() != null ? request.getRole() : UserRole.USER;
			UserStatus status = request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE;
			User user = findOrCreateUserByEmail(email, role, status);
			LinkResult linkResult = linkSocialIdempotently(user, request.getSocialType(), request.getProviderId(), email);
			if (linkResult.conflict) {
				conflict = true;
			}
			if (linkResult.created) {
				socialLinkMetrics.incrementCreate();
				result = "created";
			} else {
				socialLinkMetrics.incrementExisting();
				result = "existing";
			}

			log.info(
				"social_link provider={} providerUserId={} email={} userId={} result={} errorCode={}",
				provider,
				maskedProviderUserId,
				email,
				linkResult.userSocial.getUser().getId(),
				result,
				errorCode
			);
			userAuditLogService.logSocialLink(linkResult.userSocial, "POST /internal/users/ensure-social");
			return get(linkResult.userSocial.getUser().getId());
		} catch (BusinessException e) {
			errorCode = e.getErrorCode().name();
			log.warn(
				"social_link provider={} providerUserId={} email={} userId={} result={} errorCode={}",
				provider,
				maskedProviderUserId,
				email,
				"unknown",
				result,
				errorCode
			);
			throw e;
		} catch (Exception e) {
			errorCode = ErrorCode.INTERNAL_SERVER_ERROR.name();
			log.error(
				"social_link provider={} providerUserId={} email={} userId={} result={} errorCode={}",
				provider,
				maskedProviderUserId,
				email,
				"unknown",
				result,
				errorCode,
				e
			);
			throw e;
		} finally {
			if (conflict) {
				socialLinkMetrics.incrementConflicts();
			}
			socialLinkMetrics.recordLatency(Duration.between(startedAt, Instant.now()));
		}
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
		UserStatus before = user.getStatus();
		user.changeStatus(request.getStatus());
		userAuditLogService.logStatusChange(user, before, request.getStatus());
		return UserResponse.UserDetailResponse.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	/**
	 * 인증된 사용자의 내 정보를 조회합니다.
	 *
	 * @param userId 인증된 사용자 식별자
	 * @return active 상태 사용자 상세 응답
	 */
	public UserResponse.UserDetailResponse getMe(UUID userId) {
		User user = getUserEntityWithSocials(userId);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
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

	private User findOrCreateUserByEmail(String email, UserRole role, UserStatus status) {
		return userRepository.findWithUserSocialListByEmail(email)
			.orElseGet(() -> {
				try {
					return userRepository.save(
						User.builder()
							.email(email)
							.role(role)
							.status(status)
							.build()
					);
				} catch (DataIntegrityViolationException e) {
					return userRepository.findWithUserSocialListByEmail(email)
						.orElseThrow(() -> e);
				}
			});
	}

	private LinkResult linkSocialIdempotently(User user, UserSocialType socialType, String providerId, String email) {
		UserSocial existingSocial = userSocialRepository
			.findBySocialTypeAndProviderId(socialType, providerId)
			.orElse(null);
		if (existingSocial != null) {
			syncLinkedEmail(existingSocial, email);
			return new LinkResult(existingSocial, false, false);
		}

		try {
			UserSocial savedUserSocial = userSocialRepository.save(
				UserSocial.builder()
					.user(user)
					.socialType(socialType)
					.providerId(providerId)
					.email(email)
					.build()
			);
			user.addUserSocial(savedUserSocial);
			return new LinkResult(savedUserSocial, true, false);
		} catch (DataIntegrityViolationException e) {
			UserSocial userSocial = userSocialRepository.findBySocialTypeAndProviderId(socialType, providerId)
				.orElseThrow(() -> e);
			syncLinkedEmail(userSocial, email);
			return new LinkResult(userSocial, false, true);
		}
	}

	private void syncLinkedEmail(UserSocial userSocial, String email) {
		if (email == null) {
			return;
		}
		if (!email.equals(userSocial.getEmail())) {
			userSocial.changeEmail(email);
		}
	}

	private String hashProviderUserId(String providerUserId) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(providerUserId.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hashed.length * 2);
			for (byte b : hashed) {
				sb.append(String.format("%02x", b));
			}
			return sb.substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			return "hash_error";
		}
	}

	private static final class LinkResult {
		private final UserSocial userSocial;
		private final boolean created;
		private final boolean conflict;

		private LinkResult(UserSocial userSocial, boolean created, boolean conflict) {
			this.userSocial = userSocial;
			this.created = created;
			this.conflict = conflict;
		}
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
