package com.api.config.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();

		addRoleAuthorities(jwt, authorities);
		addScopeAuthorities(jwt, authorities);

		return authorities;
	}

	private void addRoleAuthorities(Jwt jwt, Set<GrantedAuthority> authorities) {
		Object roleClaim = jwt.getClaims().get("role");
		if (roleClaim instanceof String role && !role.isBlank()) {
			authorities.add(new SimpleGrantedAuthority(toRoleAuthority(role)));
		}

		Object rolesClaim = jwt.getClaims().get("roles");
		if (rolesClaim instanceof List<?> roles) {
			roles.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.map(this::toRoleAuthority)
				.map(SimpleGrantedAuthority::new)
				.forEach(authorities::add);
		}
	}

	private void addScopeAuthorities(Jwt jwt, Set<GrantedAuthority> authorities) {
		Object scopeClaim = jwt.getClaims().get("scope");
		if (scopeClaim instanceof String scope && !scope.isBlank()) {
			for (String value : scope.split("\\s+")) {
				if (!value.isBlank()) {
					authorities.add(new SimpleGrantedAuthority("SCOPE_" + value));
				}
			}
		}

		Object scpClaim = jwt.getClaims().get("scp");
		if (scpClaim instanceof List<?> scopes) {
			scopes.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.map(scope -> "SCOPE_" + scope)
				.map(SimpleGrantedAuthority::new)
				.forEach(authorities::add);
		}
	}

	private String toRoleAuthority(String role) {
		return role.startsWith("ROLE_") ? role : "ROLE_" + role;
	}
}
