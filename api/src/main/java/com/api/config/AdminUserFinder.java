package com.api.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import auth.UserFinder;

import com.api.user.repository.UserRepository;
import com.auth.model.User;

/**
 * auth 모듈의 {@link UserFinder} 구현체입니다.
 *
 * <p>관리자 사용자 저장소에서 username으로 사용자를 조회해
 * auth 모듈이 이해할 수 있는 {@link User} 모델로 변환합니다.</p>
 */
@Component
public class AdminUserFinder implements UserFinder {

	private final UserRepository userRepository;

	/**
	 * 생성자 주입입니다.
	 *
	 * @param userRepository 관리자 사용자 저장소
	 */
	public AdminUserFinder(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * username으로 사용자를 조회합니다.
	 *
	 * @param username 로그인 아이디
	 * @return auth 모듈 사용자 모델(Optional)
	 */
	@Override
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username)
			.map(e -> new User(
				String.valueOf(e.getId()),
				e.getUsername(),
				e.getPassword(),
				List.of(e.getRole().authority())
			));
	}
}
