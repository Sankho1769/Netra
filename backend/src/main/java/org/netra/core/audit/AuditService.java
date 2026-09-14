package org.netra.core.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final EligibilityAuditLogRepository auditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;

    public AuditService(
            EligibilityAuditLogRepository auditLogRepository,
            SecurityAuditLogRepository securityAuditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.securityAuditLogRepository = securityAuditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthEvent(String eventType, UUID userId, String clientIp, String userAgent, String metadata) {
        String ipHash = hashIp(clientIp);
        SecurityAuditLog auditLog = new SecurityAuditLog(
                eventType,
                userId,
                ipHash,
                userAgent,
                metadata
        );
        securityAuditLogRepository.save(auditLog);
        log.info("Security audit event: type={}, userId={}", eventType, userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String eventType, UUID userId, UUID sessionId, String ruleVersion, String resultType, String clientIp, String userAgent) {
        String ipHash = hashIp(clientIp);
        EligibilityAuditLog auditLog = new EligibilityAuditLog(
                eventType,
                userId,
                sessionId,
                ruleVersion,
                resultType,
                ipHash,
                userAgent
        );

        auditLogRepository.save(auditLog);
        log.info("Audit event logged: type={}, sessionId={}, ruleVersion={}, result={}", 
                eventType, sessionId, ruleVersion, resultType);
    }

    public String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "HASH_ERROR";
        }
    }
}
