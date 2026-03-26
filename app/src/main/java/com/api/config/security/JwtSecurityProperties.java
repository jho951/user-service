package com.api.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * JWT 검증에 필요한 보안 설정값을 바인딩합니다.
 *
 * @param enabled JWT 보안 활성화 여부
 * @param issuer issuer claim 값
 * @param secret HMAC 시크릿
 * @param audience aud claim 값
 * @param internalScope 내부 호출용 scope 값
 */
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtSecurityProperties(
	boolean enabled,
	@NotBlank(message = "security.jwt.issuer must be configured") String issuer,
	@NotBlank(message = "security.jwt.secret must be configured") String secret,
	@NotBlank(message = "security.jwt.audience must be configured") String audience,
	@NotBlank(message = "security.jwt.internal-scope must be configured") String internalScope
) {
}
