package org.netra.features.karma.repository;

import org.netra.features.karma.entity.KarmaEventType;
import org.netra.features.karma.entity.KarmaTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KarmaTransactionRepository extends JpaRepository<KarmaTransaction, UUID> {

    Page<KarmaTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<KarmaTransaction> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByReferenceTypeAndReferenceIdAndEventType(
            String referenceType, String referenceId, KarmaEventType eventType);

    Optional<KarmaTransaction> findByReferenceTypeAndReferenceIdAndEventType(
            String referenceType, String referenceId, KarmaEventType eventType);

    long countByUserId(UUID userId);
}
