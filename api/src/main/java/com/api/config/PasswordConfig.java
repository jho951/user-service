package com.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더 빈 설정입니다.
 */
@Configuration
public class PasswordConfig {
	/**
	 * BCrypt 기반 {@link PasswordEncoder}를 등록합니다.
	 *
	 * @return 패스워드 인코더
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
