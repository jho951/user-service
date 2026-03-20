package com.api.config.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.core.constant.UserRole;

/**
 * JWT claim을 Spring Security 권한 객체로 변환합니다.
 */
public class JwtAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	/**
	 * role, roles, scope, scp claim을 권한 목록으로 변환합니다.
	 *
	 * @param jwt JWT 토큰
	 * @return 권한 목록
	 */
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();

		addRoleAuthorities(jwt, authorities);
		addScopeAuthorities(jwt, authorities);

		return authorities;
	}

	/**
	 * role 및 roles claim을 Spring Security 권한으로 추가합니다.
	 *
	 * @param jwt JWT 토큰
	 * @param authorities 권한 누적 컬렉션
	 */
	private void addRoleAuthorities(Jwt jwt, Set<GrantedAuthority> authorities) {
		Object roleClaim = jwt.getClaims().get("role");
		if (roleClaim instanceof String role && !role.isBlank()) {
			authorities.add(toGrantedAuthority(role));
		}

		Object rolesClaim = jwt.getClaims().get("roles");
		if (rolesClaim instanceof List<?> roles) {
			roles.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.map(this::toGrantedAuthority)
				.forEach(authorities::add);
		}
	}

	/**
	 * scope 및 scp claim을 Spring Security 권한으로 추가합니다.
	 *
	 * @param jwt JWT 토큰
	 * @param authorities 권한 누적 컬렉션
	 */
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

	/**
	 * role 문자열을 Spring Security 권한 객체로 변환합니다.
	 *
	 * @param role role 또는 authority 문자열
	 * @return 변환된 권한 객체
	 */
	private GrantedAuthority toGrantedAuthority(String role) {
		return new SimpleGrantedAuthority(UserRole.from(role).authority());
	}
}
