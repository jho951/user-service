package com.api.user.audit;

import com.api.user.constant.UserSocialType;
import com.api.user.constant.UserStatus;
import com.api.user.entity.User;
import com.api.user.entity.UserSocial;
import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditLogger;
import com.auditlog.api.AuditResult;
import com.auditlog.api.AuditSink;
import com.auditlog.core.DefaultAuditLogger;
import com.auditlog.core.FileAuditSink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserAuditLogService {
    private final boolean enabled;
    private final AuditLogger logger;

    public UserAuditLogService(
            @Value("${auditlog.enabled:true}") boolean enabled,
            @Value("${auditlog.service-name:user-service}") String serviceName,
            @Value("${auditlog.env:local}") String env,
            @Value("${auditlog.file-path:./logs/audit.log}") String filePath
    ) {
        this.enabled = enabled;
        if (!enabled) {
            this.logger = null;
            return;
        }

        AuditSink sink = new FileAuditSink(Path.of(filePath), serviceName, env);
        this.logger = new DefaultAuditLogger(sink, List.of(), null);
    }

    public void logSignup(User user) {
        emit(
                AuditEventType.CREATE,
                "USER_SIGNUP",
                "anonymous",
                AuditActorType.ANONYMOUS,
                "USER_ACCOUNT",
                user == null ? "unknown" : user.getId().toString(),
                AuditResult.SUCCESS,
                "PUBLIC_SIGNUP",
                null,
                user == null ? null : user.getEmail()
        );
    }

    public void logInternalCreate(User user) {
        emit(
                AuditEventType.CREATE,
                "USER_INTERNAL_CREATE",
                "internal-service",
                AuditActorType.SERVICE,
                "USER_ACCOUNT",
                user.getId().toString(),
                AuditResult.SUCCESS,
                "INTERNAL_CREATE",
                "role=" + user.getRole() + ",status=" + user.getStatus(),
                user.getEmail()
        );
    }

    public void logStatusChange(User user, UserStatus before, UserStatus after) {
        emit(
                AuditEventType.UPDATE,
                "USER_STATUS_UPDATE",
                "internal-service",
                AuditActorType.SERVICE,
                "USER_ACCOUNT",
                user.getId().toString(),
                AuditResult.SUCCESS,
                before + "->" + after,
                "before=" + before + ",after=" + after,
                user.getEmail()
        );
    }

    public void logSocialLink(UserSocial userSocial, String source) {
        emit(
                userSocial != null && userSocial.getId() != null ? AuditEventType.CREATE : AuditEventType.UPDATE,
                "USER_SOCIAL_LINK",
                "internal-service",
                AuditActorType.SERVICE,
                "USER_SOCIAL_ACCOUNT",
                userSocial == null || userSocial.getId() == null ? "unknown" : userSocial.getId().toString(),
                AuditResult.SUCCESS,
                source,
                "socialType=" + (userSocial == null ? "unknown" : userSocial.getSocialType())
                        + ",providerIdHash=" + (userSocial == null ? "unknown" : hashProviderUserId(userSocial.getProviderId())),
                userSocial == null ? null : userSocial.getEmail()
        );
    }

    private void emit(
            AuditEventType eventType,
            String eventName,
            String actorId,
            AuditActorType actorType,
            String resourceType,
            String resourceId,
            AuditResult result,
            String reason,
            String details,
            String piiSeed
    ) {
        if (!enabled || logger == null) {
            return;
        }

        var builder = AuditEvent.builder(eventType, eventName)
                .actor(actorId, actorType, null)
                .resource(resourceType, resourceId)
                .result(result)
                .reason(reason);
        if (details != null && !details.isBlank()) {
            builder.detail("details", details);
        }
        if (piiSeed != null && !piiSeed.isBlank()) {
            builder.detail("emailHash", hashProviderUserId(piiSeed));
        }
        logger.log(builder.build());
    }

    private String hashProviderUserId(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
    }
}
