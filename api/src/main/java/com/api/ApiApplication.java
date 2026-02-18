package com.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Admin API 애플리케이션의 시작점입니다.
 *
 * <p>JPA Auditing을 활성화해 {@code BaseEntity}의 생성/수정 시각을 자동 관리합니다.</p>
 */
@EnableJpaAuditing
@SpringBootApplication
public class ApiApplication {
	/**
	 * Spring Boot 애플리케이션을 실행합니다.
	 *
	 * @param args 실행 인자
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}
}
