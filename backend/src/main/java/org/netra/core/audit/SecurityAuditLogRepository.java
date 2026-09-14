package org.netra.core.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {
    List<SecurityAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<SecurityAuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
