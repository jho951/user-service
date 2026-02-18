package com.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.user.domain.User;

import com.core.constant.UserRole;

/**
 * 관리자 사용자 엔티티 저장소입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 이메일 중복 여부를 확인합니다.
	 *
	 * @param email 이메일
	 * @return 존재 여부
	 */
	boolean existsByEmail(String email);

	/**
	 * 특정 권한 사용자의 존재 여부를 확인합니다.
	 *
	 * @param role 권한
	 * @return 존재 여부
	 */
	boolean existsByRole(UserRole role);

	/**
	 * 이메일로 사용자를 조회합니다.
	 *
	 * @param email 이메일
	 * @return 사용자(Optional)
	 */
	Optional<User> findByEmail(String email);

	/**
	 * username으로 사용자를 조회합니다.
	 *
	 * @param username 아이디
	 * @return 사용자(Optional)
	 */
	Optional<User> findByUsername(String username);
}
