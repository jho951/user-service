package com.userservice.app.domain.audit;

import com.userservice.app.domain.user.constant.UserStatus;
import com.userservice.app.domain.user.entity.User;
import com.userservice.app.domain.user.entity.UserSocial;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Service
public class UserAuditLogService {
    private final GovernanceAuditRecorder auditRecorder;

    public UserAuditLogService(GovernanceAuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    public void logSignup(User user) {
        emit(
                "CREATE",
                "USER_SIGNUP",
                "anonymous",
                "ANONYMOUS",
                "USER_ACCOUNT",
                user == null ? "unknown" : user.getId().toString(),
                "SUCCESS",
                "PUBLIC_SIGNUP",
                null,
                user == null ? null : user.getEmail()
        );
    }

    public void logInternalCreate(User user) {
        emit(
                "CREATE",
                "USER_INTERNAL_CREATE",
                "internal-service",
                "SERVICE",
                "USER_ACCOUNT",
                user.getId().toString(),
                "SUCCESS",
                "INTERNAL_CREATE",
                "role=" + user.getRole() + ",status=" + user.getStatus(),
                user.getEmail()
        );
    }

    public void logStatusChange(User user, UserStatus before, UserStatus after) {
        emit(
                "UPDATE",
                "USER_STATUS_UPDATE",
                "internal-service",
                "SERVICE",
                "USER_ACCOUNT",
                user.getId().toString(),
                "SUCCESS",
                before + "->" + after,
                "before=" + before + ",after=" + after,
                user.getEmail()
        );
    }

    public void logSocialLink(UserSocial userSocial, String source) {
        emit(
                userSocial != null && userSocial.getId() != null ? "CREATE" : "UPDATE",
                "USER_SOCIAL_LINK",
                "internal-service",
                "SERVICE",
                "USER_SOCIAL_ACCOUNT",
                userSocial == null || userSocial.getId() == null ? "unknown" : userSocial.getId().toString(),
                "SUCCESS",
                source,
                "socialType=" + (userSocial == null ? "unknown" : userSocial.getSocialType())
                        + ",providerIdHash=" + (userSocial == null ? "unknown" : hashProviderUserId(userSocial.getProviderId())),
                userSocial == null ? null : userSocial.getEmail()
        );
    }

    private void emit(
            String eventType,
            String eventName,
            String actorId,
            String actorType,
            String resourceType,
            String resourceId,
            String result,
            String reason,
            String details,
            String piiSeed
    ) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("eventType", eventType);
        attributes.put("actorId", actorId);
        attributes.put("actorType", actorType);
        attributes.put("resourceType", resourceType);
        attributes.put("resourceId", resourceId);
        attributes.put("result", result);
        attributes.put("reason", reason);
        if (details != null && !details.isBlank()) {
            attributes.put("details", details);
        }
        if (piiSeed != null && !piiSeed.isBlank()) {
            attributes.put("emailHash", hashProviderUserId(piiSeed));
        }
        auditRecorder.record(new AuditEntry(
                "user",
                eventName,
                Map.copyOf(attributes),
                Instant.now()
        ));
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
