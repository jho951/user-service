package com.userservice.app.config;

import com.auditlog.api.AuditSink;
import io.github.jho951.platform.security.policy.PlatformSecurityProperties;
import io.github.jho951.ratelimiter.core.RateLimitDecision;
import io.github.jho951.ratelimiter.core.RateLimitKey;
import io.github.jho951.ratelimiter.core.RateLimitPlan;
import io.github.jho951.ratelimiter.spi.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
public class UserPlatformRuntimeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UserPlatformRuntimeConfiguration.class);

    @Bean
    public AuditSink userGovernanceAuditSink() {
        return event -> log.info(
            "governanceAudit eventId={} action={} resourceType={} resourceId={} result={} reason={} requestId={} traceId={}",
            event.getEventId(),
            event.getAction(),
            event.getResourceType(),
            event.getResourceId(),
            event.getResult(),
            event.getReason(),
            event.getRequestId(),
            event.getTraceId()
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
    public RateLimiter platformSecurityRateLimiter(
        StringRedisTemplate redisTemplate,
        @Value("${PLATFORM_SECURITY_RATE_LIMIT_REDIS_PREFIX:platform-security:rate-limit:user-service:}")
        String keyPrefix
    ) {
        return new RedisFixedWindowRateLimiter(redisTemplate, keyPrefix, Clock.systemUTC());
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

    private static final class RedisFixedWindowRateLimiter implements RateLimiter {
        private final StringRedisTemplate redisTemplate;
        private final String keyPrefix;
        private final Clock clock;

        private RedisFixedWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            String keyPrefix,
            Clock clock
        ) {
            this.redisTemplate = redisTemplate;
            this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
            this.clock = clock;
        }

        @Override
        public RateLimitDecision tryAcquire(RateLimitKey key, long permits, RateLimitPlan plan) {
            long windowSeconds = Math.max(1L, (long) Math.ceil(plan.getCapacity() / plan.getRefillTokensPerSecond()));
            long nowSeconds = clock.instant().getEpochSecond();
            long windowIndex = nowSeconds / windowSeconds;
            long windowEndSeconds = (windowIndex + 1L) * windowSeconds;
            String redisKey = keyPrefix + key.asString() + ":" + windowIndex;

            Long current = redisTemplate.opsForValue().increment(redisKey, permits);
            if (current == null) {
                return RateLimitDecision.deny(0L, windowSeconds * 1000L);
            }
            if (current == permits) {
                redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(windowSeconds + 1L));
            }

            long remaining = Math.max(0L, plan.getCapacity() - current);
            if (current <= plan.getCapacity()) {
                return RateLimitDecision.allow(remaining);
            }

            long retryAfterMillis = Math.max(0L, (windowEndSeconds - nowSeconds) * 1000L);
            return RateLimitDecision.deny(remaining, retryAfterMillis);
        }
    }
}
