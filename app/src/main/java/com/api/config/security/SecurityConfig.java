package com.api.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.api.common.dto.GlobalResponse;
import com.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * user-service의 Spring Security 설정을 구성합니다.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

	@Bean
	/**
	 * 보안 필터 체인을 구성합니다.
	 *
	 * @param http HttpSecurity 설정 객체
	 * @param jwtAuthenticationConverter JWT 인증 변환기
	 * @param jwtAccessPolicy JWT 접근 정책
	 * @param objectMapper JSON 직렬화 객체
	 * @return 보안 필터 체인
	 * @throws Exception 보안 구성 중 예외
	 */
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JwtAuthenticationConverter jwtAuthenticationConverter,
		JwtAccessPolicy jwtAccessPolicy,
		ObjectMapper objectMapper
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/users/signup").permitAll()
				.requestMatchers("/internal/users/**").access(
					(authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(
						jwtAccessPolicy.hasInternalScope(authentication.get())
					)
				)
				.requestMatchers("/users/me").authenticated()
				.anyRequest().denyAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, authException) ->
					writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, GlobalResponse.fail(ErrorCode.UNAUTHORIZED)))
				.accessDeniedHandler((request, response, accessDeniedException) ->
					writeError(response, objectMapper, HttpStatus.FORBIDDEN, GlobalResponse.fail(ErrorCode.FORBIDDEN)))
			)
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	@Bean
	/**
	 * JWT 디코더를 생성합니다.
	 *
	 * @param properties JWT 보안 설정
	 * @return JWT 디코더
	 */
	public JwtDecoder jwtDecoder(JwtSecurityProperties properties) {
		SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
		OAuth2TokenValidator<Jwt> withSubject = jwt -> {
			String subject = jwt.getSubject();
			if (subject == null || subject.isBlank()) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "sub claim is required", null));
			}
			return OAuth2TokenValidatorResult.success();
		};
		OAuth2TokenValidator<Jwt> withAudience = jwt -> {
			List<String> audience = jwt.getAudience();
			if (audience != null && audience.contains(properties.audience())) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(
				new OAuth2Error("invalid_token", "aud claim does not include required audience", null)
			);
		};

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(withIssuer, withSubject, withAudience)));
		return decoder;
	}

	@Bean
	/**
	 * JWT를 Spring Security 인증 객체로 변환하는 컨버터를 생성합니다.
	 *
	 * @return JWT 인증 컨버터
	 */
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtAuthorityConverter());
		converter.setPrincipalClaimName("sub");
		return converter;
	}

	/**
	 * 보안 예외 응답을 공통 JSON 형식으로 기록합니다.
	 *
	 * @param response 서블릿 응답 객체
	 * @param objectMapper JSON 직렬화 객체
	 * @param status HTTP 상태 코드
	 * @param body 응답 바디
	 * @throws IOException 응답 기록 중 예외
	 */
	private static void writeError(
		jakarta.servlet.http.HttpServletResponse response,
		ObjectMapper objectMapper,
		HttpStatus status,
		GlobalResponse<Void> body
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), body);
	}
}
