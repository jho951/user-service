package com.api.user.service;

import com.api.user.domain.User;
import com.api.user.dto.UserRequestDto;
import com.api.user.repository.UserRepository;
import com.core.constant.UserRole;
import com.core.exception.BusinessException;
import com.core.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 사용자 관리 비즈니스 로직 구현체입니다.
 */
@Service
@Transactional
public class UserService implements UserServiceInterface {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 생성자 주입입니다.
	 *
	 * @param userRepository 사용자 저장소
	 * @param passwordEncoder 비밀번호 인코더
	 */
	public UserService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 관리자 사용자 전체를 조회합니다.
	 *
	 * @return 사용자 목록
	 */
	@Override
	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userRepository.findAll();
	}

	/**
	 * 사용자 ID로 조회하고, 없으면 예외를 던집니다.
	 *
	 * @param id 사용자 ID
	 * @return 사용자 엔티티
	 */
	@Override
	@Transactional(readOnly = true)
	public User findByIdOrThrow(Long id) {
		return userRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 관리자 사용자를 생성합니다.
	 *
	 * @param request 생성 요청 DTO
	 * @return 생성된 사용자
	 */
	@Override
	public User create(UserRequestDto request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());

		User user = User.builder()
			.username(request.getUsername())
			.password(encodedPassword)
			.email(request.getEmail())
			.role(request.getRole() != null ? request.getRole() : UserRole.ADMIN)
			.enabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE)
			.build();

		return userRepository.save(user);
	}

	/**
	 * 관리자 사용자 정보를 수정합니다.
	 *
	 * @param id 사용자 ID
	 * @param request 수정 요청 DTO
	 * @return 수정된 사용자
	 */
	@Override
	public User update(Long id, UserRequestDto request) {
		User user = findByIdOrThrow(id);

		if (request.getEmail() != null) {
			user.changeEmail(request.getEmail());
		}

		if (request.getRole() != null) {
			user.changeRole(request.getRole());
		}

		if (request.getEnabled() != null) {
			user.changeEnabled(request.getEnabled());
		}

		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			String encodedPassword = passwordEncoder.encode(request.getPassword());
			user.changePassword(encodedPassword);
		}

		return user;
	}

	/**
	 * 관리자 사용자를 삭제합니다.
	 *
	 * @param id 사용자 ID
	 */
	@Override
	public void delete(Long id) {
		User user = findByIdOrThrow(id);
		userRepository.delete(user);
	}
}
