package com.api.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.api.user.constant.UserStatus;

@Component("jwtAccessPolicy")
public class JwtAccessPolicy {

	private final JwtSecurityProperties properties;

	public JwtAccessPolicy(JwtSecurityProperties properties) {
		this.properties = properties;
	}

	public boolean hasActiveStatus(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
			return false;
		}

		String status = jwtAuthenticationToken.getToken().getClaimAsString("status");
		return UserStatus.ACTIVE.name().equals(status);
	}

	public boolean hasInternalScope(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
			return false;
		}

		String requiredAuthority = "SCOPE_" + properties.internalScope();
		return jwtAuthenticationToken.getAuthorities().stream()
			.anyMatch(grantedAuthority -> requiredAuthority.equals(grantedAuthority.getAuthority()));
	}
}
