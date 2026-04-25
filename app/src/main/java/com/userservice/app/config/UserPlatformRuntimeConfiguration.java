package com.userservice.app.config;

import io.github.jho951.platform.governance.api.GovernanceAuditSink;
import io.github.jho951.platform.security.policy.PlatformSecurityProperties;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitDecision;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitKeyType;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitPort;
import io.github.jho951.platform.security.ratelimit.PlatformRateLimitRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class UserPlatformRuntimeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UserPlatformRuntimeConfiguration.class);

    @Bean
    public GovernanceAuditSink userGovernanceAuditSink() {
        return entry -> log.info(
            "governanceAudit category={} message={} requestId={} traceId={}",
            entry.category(),
            entry.message(),
            entry.attributes().getOrDefault("requestId", ""),
            entry.attributes().getOrDefault("traceId", "")
        );
    }

    @Bean
    public JwtDecoder userServiceJwtDecoder(PlatformSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        decoder.setJwtValidator(buildJwtValidator(properties.getAuth()));
        return decoder;
    }

    @Bean
    @Profile({"prod", "production", "live"})
    public PlatformRateLimitPort platformSecurityRateLimiter(
        StringRedisTemplate redisTemplate,
        @Value("${PLATFORM_SECURITY_RATE_LIMIT_REDIS_PREFIX:platform-security:rate-limit:user-service:}")
        String keyPrefix
    ) {
        return new RedisFixedWindowPlatformRateLimitPort(redisTemplate, keyPrefix, Clock.systemUTC());
    }

    private OAuth2TokenValidator<Jwt> buildJwtValidator(PlatformSecurityProperties.AuthProperties auth) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        String issuer = trimToNull(auth.getJwtIssuer());
        validators.add(issuer == null ? JwtValidators.createDefault() : JwtValidators.createDefaultWithIssuer(issuer));

        String audience = trimToNull(auth.getJwtAudience());
        if (audience != null) {
            validators.add(jwt -> {
                List<String> audiences = jwt.getAudience();
                if (audiences != null && audiences.contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "JWT audience does not include " + audience, null)
                );
            });
        }
        return token -> {
            for (OAuth2TokenValidator<Jwt> validator : validators) {
                OAuth2TokenValidatorResult result = validator.validate(token);
                if (result.hasErrors()) {
                    return result;
                }
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class RedisFixedWindowPlatformRateLimitPort implements PlatformRateLimitPort {
        private final StringRedisTemplate redisTemplate;
        private final String keyPrefix;
        private final Clock clock;

        private RedisFixedWindowPlatformRateLimitPort(
            StringRedisTemplate redisTemplate,
            String keyPrefix,
            Clock clock
        ) {
            this.redisTemplate = redisTemplate;
            this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
            this.clock = clock;
        }

        @Override
        public PlatformRateLimitDecision evaluate(PlatformRateLimitRequest request) {
            long windowSeconds = Math.max(1L, request.windowSeconds());
            long nowSeconds = clock.instant().getEpochSecond();
            long windowIndex = nowSeconds / windowSeconds;
            long windowEndSeconds = (windowIndex + 1L) * windowSeconds;
            String redisKey = keyPrefix
                + keyTypeSegment(request.keyType())
                + ":"
                + request.key()
                + ":"
                + windowIndex;

            Long current = redisTemplate.opsForValue().increment(redisKey, request.permits());
            if (current == null) {
                return PlatformRateLimitDecision.deny(request.key(), "rate limit backend unavailable");
            }
            if (current == request.permits()) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 1L));
            }

            if (current <= request.limit()) {
                return PlatformRateLimitDecision.allow(request.key(), "within rate limit");
            }

            long retryAfterSeconds = Math.max(0L, windowEndSeconds - nowSeconds);
            return PlatformRateLimitDecision.deny(
                request.key(),
                "rate limit exceeded for " + request.key() + "; retry_after_seconds=" + retryAfterSeconds
            );
        }

        private String keyTypeSegment(PlatformRateLimitKeyType keyType) {
            return keyType == PlatformRateLimitKeyType.USER ? "user" : "ip";
        }
    }
}
