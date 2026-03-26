package com.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring 컨텍스트 로딩 테스트입니다.
 */
@SpringBootTest(properties = {
	"USER_SERVICE_BASE_URL=http://localhost:8082",
	"USER_SERVICE_INTERNAL_JWT_ISSUER=auth-service",
	"USER_SERVICE_INTERNAL_JWT_AUDIENCE=user-service",
	"USER_SERVICE_INTERNAL_JWT_SECRET=abcdefghijklmnopqrstuvwxyz12345678901234567890"
})
class ApiApplicationTests {

	/**
	 * 애플리케이션 컨텍스트가 정상적으로 로딩되는지 검증합니다.
	 */
	@Test
	void contextLoads() {
	}

}
