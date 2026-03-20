package com.api.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.api.user.constant.UserStatus;

@Component("jwtAccessPolicy")
/**
 * JWT의 상태 및 scope 기반 접근 정책을 검사합니다.
 */
public class JwtAccessPolicy {

	private final JwtSecurityProperties properties;

	/**
	 * JWT 접근 정책 검사기를 생성합니다.
	 *
	 * @param properties JWT 보안 설정
	 */
	public JwtAccessPolicy(JwtSecurityProperties properties) {
		this.properties = properties;
	}

	/**
	 * 인증된 사용자의 상태가 ACTIVE인지 확인합니다.
	 *
	 * @param authentication 인증 정보
	 * @return ACTIVE 상태 여부
	 */
	public boolean hasActiveStatus(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
			return false;
		}

		String status = jwtAuthenticationToken.getToken().getClaimAsString("status");
		return UserStatus.ACTIVE.name().equals(status);
	}

	/**
	 * 내부 서비스 호출에 필요한 scope가 포함되어 있는지 확인합니다.
	 *
	 * @param authentication 인증 정보
	 * @return 내부 scope 보유 여부
	 */
	public boolean hasInternalScope(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
			return false;
		}

		String requiredAuthority = "SCOPE_" + properties.internalScope();
		return jwtAuthenticationToken.getAuthorities().stream()
			.anyMatch(grantedAuthority -> requiredAuthority.equals(grantedAuthority.getAuthority()));
	}
}
