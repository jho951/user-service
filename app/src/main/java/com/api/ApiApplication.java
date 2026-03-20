package com.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** User Service 시작점입니다. */
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
