package com.api.config;

import com.api.user.domain.User;
import com.api.user.repository.UserRepository;
import com.core.constant.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 기본 SUPER_ADMIN 계정을 보장합니다.
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${admin.super.username:superadmin}")
	private String superUsername;

	@Value("${admin.super.email:superadmin@example.com}")
	private String superEmail;

	@Value("${admin.super.password:superadmin1234}")
	private String superPassword;

	/**
	 * 생성자 주입입니다.
	 *
	 * @param userRepository 관리자 사용자 저장소
	 * @param passwordEncoder 비밀번호 인코더
	 */
	public SuperAdminInitializer(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 기본 최고 관리자 계정이 없을 때만 생성합니다.
	 *
	 * @param args 실행 인자
	 */
	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
			return;
		}
		if (userRepository.existsByEmail(superEmail)) {
			return;
		}
		User superAdmin = User.builder()
			.username(superUsername)
			.email(superEmail)
			.password(passwordEncoder.encode(superPassword))
			.role(UserRole.SUPER_ADMIN)
			.enabled(true)
			.build();

		userRepository.save(superAdmin);
	}
}
