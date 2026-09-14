package org.netra.core.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EligibilityAuditLogRepository extends JpaRepository<EligibilityAuditLog, UUID> {
    List<EligibilityAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<EligibilityAuditLog> findBySessionId(UUID sessionId);
}
