package com.api.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtSecurityProperties(
	boolean enabled,
	String issuer,
	String secret,
	String audience,
	String internalScope
) {
}
