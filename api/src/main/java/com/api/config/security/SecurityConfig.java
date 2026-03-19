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

import com.core.dto.BaseResponse;
import com.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

	@Bean
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
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
				.requestMatchers("/internal/users/**").access(
					(authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(
						jwtAccessPolicy.hasInternalScope(authentication.get())
					)
				)
				.requestMatchers("/api/users/me").authenticated()
				.anyRequest().denyAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, authException) ->
					writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, BaseResponse.error(ErrorCode.UNAUTHORIZED)))
				.accessDeniedHandler((request, response, accessDeniedException) ->
					writeError(response, objectMapper, HttpStatus.FORBIDDEN, BaseResponse.error(ErrorCode.FORBIDDEN)))
			)
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	@Bean
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
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtAuthorityConverter());
		converter.setPrincipalClaimName("sub");
		return converter;
	}

	private static void writeError(
		jakarta.servlet.http.HttpServletResponse response,
		ObjectMapper objectMapper,
		HttpStatus status,
		BaseResponse<Void> body
	) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), body);
	}
}
