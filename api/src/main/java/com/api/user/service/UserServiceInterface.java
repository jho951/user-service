package com.api.user.service;

import com.api.user.domain.User;
import com.api.user.dto.UserRequestDto;

import java.util.List;

/**
 * 관리자 사용자 서비스 인터페이스입니다.
 */
public interface UserServiceInterface {

	/**
	 * 모든 관리자 계정을 조회합니다.
	 *
	 * @return 관리자 계정 목록
	 */
	List<User> findAll();

	/**
	 * ID로 관리자 계정을 조회합니다.
	 *
	 * @param id 사용자 ID
	 * @return 관리자 계정
	 */
	User findByIdOrThrow(Long id);

	/**
	 * 관리자 계정을 생성합니다.
	 *
	 * @param request 생성 요청
	 * @return 생성된 관리자 계정
	 */
	User create(UserRequestDto request);

	/**
	 * 관리자 계정을 수정합니다.
	 *
	 * @param id 사용자 ID
	 * @param request 수정 요청
	 * @return 수정된 관리자 계정
	 */
	User update(Long id, UserRequestDto request);

	/**
	 * 관리자 계정을 삭제합니다.
	 *
	 * @param id 사용자 ID
	 */
	void delete(Long id);
}
