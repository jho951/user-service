package com.api.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.api.common.dto.GlobalResponse;
import com.api.config.security.gateway.GatewayUserContextFilter;
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
	@Order(1)
	/**
	 * 내부 API(`/internal/**`) 보안 체인을 구성합니다.
	 *
	 * @param http HttpSecurity 설정 객체
	 * @param jwtAuthenticationConverter JWT 인증 변환기
	 * @param jwtAccessPolicy JWT 접근 정책
	 * @param objectMapper JSON 직렬화 객체
	 * @return 보안 필터 체인
	 * @throws Exception 보안 구성 중 예외
	 */
	public SecurityFilterChain internalApiSecurityFilterChain(
		HttpSecurity http,
		JwtAuthenticationConverter jwtAuthenticationConverter,
		JwtAccessPolicy jwtAccessPolicy,
		ObjectMapper objectMapper,
		@Qualifier("internalJwtDecoder") JwtDecoder internalJwtDecoder
	) throws Exception {
		http
			.securityMatcher("/internal/**")
			.cors(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/internal/**").access(
					(authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(
						jwtAccessPolicy.hasInternalScope(authentication.get())
					)
				)
				.anyRequest().denyAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
				.decoder(internalJwtDecoder)
				.jwtAuthenticationConverter(jwtAuthenticationConverter)
			))
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(unauthorizedEntryPoint(objectMapper))
				.accessDeniedHandler(forbiddenHandler(objectMapper))
			)
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	@Bean
	@Order(2)
	/**
	 * 사용자 API(`/users/**`) 보안 체인을 구성합니다.
	 * gateway 사용자 컨텍스트 헤더와 Bearer 사용자 토큰을 함께 허용합니다.
	 *
	 * @param http HttpSecurity 설정 객체
	 * @param objectMapper JSON 직렬화 객체
	 * @param gatewayUserContextFilter gateway 사용자 컨텍스트 필터
	 * @param userJwtDecoder 사용자 토큰 디코더
	 * @return 보안 필터 체인
	 * @throws Exception 보안 구성 중 예외
	 */
	public SecurityFilterChain userApiSecurityFilterChain(
		HttpSecurity http,
		ObjectMapper objectMapper,
		GatewayUserContextFilter gatewayUserContextFilter,
		@Qualifier("userJwtDecoder") JwtDecoder userJwtDecoder
	) throws Exception {
		http
			.securityMatcher("/users/**")
			.cors(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/users/signup").permitAll()
				.requestMatchers("/users/me").authenticated()
				.anyRequest().denyAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(userJwtDecoder)))
			.addFilterAfter(gatewayUserContextFilter, BearerTokenAuthenticationFilter.class)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(unauthorizedEntryPoint(objectMapper))
				.accessDeniedHandler(forbiddenHandler(objectMapper))
			)
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	@Bean
	@Order(3)
	/**
	 * 명시되지 않은 경로를 기본 차단합니다.
	 *
	 * @param http HttpSecurity 설정 객체
	 * @return 보안 필터 체인
	 * @throws Exception 보안 구성 중 예외
	 */
	public SecurityFilterChain defaultDenySecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());

		return http.build();
	}

	@Bean("internalJwtDecoder")
	/**
	 * 내부 서비스 호출 JWT 디코더를 생성합니다.
	 * `iss`, `aud`, `sub`를 엄격히 검증합니다.
	 *
	 * @param properties JWT 보안 설정
	 * @return JWT 디코더
	 */
	public JwtDecoder internalJwtDecoder(JwtSecurityProperties properties) {
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

	@Bean("userJwtDecoder")
	/**
	 * 사용자 API용 JWT 디코더를 생성합니다.
	 * gateway를 경유한 사용자 토큰을 위해 sub 존재 여부만 강제합니다.
	 *
	 * @param properties JWT 보안 설정
	 * @return JWT 디코더
	 */
	public JwtDecoder userJwtDecoder(JwtSecurityProperties properties) {
		SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

		OAuth2TokenValidator<Jwt> withSubject = jwt -> {
			String subject = jwt.getSubject();
			if (subject == null || subject.isBlank()) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "sub claim is required", null));
			}
			return OAuth2TokenValidatorResult.success();
		};

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(JwtValidators.createDefault(), withSubject)));
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

	private AuthenticationEntryPoint unauthorizedEntryPoint(ObjectMapper objectMapper) {
		return (request, response, authException) ->
			writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, GlobalResponse.fail(ErrorCode.UNAUTHORIZED));
	}

	private AccessDeniedHandler forbiddenHandler(ObjectMapper objectMapper) {
		return (request, response, accessDeniedException) ->
			writeError(response, objectMapper, HttpStatus.FORBIDDEN, GlobalResponse.fail(ErrorCode.FORBIDDEN));
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
