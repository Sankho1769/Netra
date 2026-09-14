package org.netra.features.eligibility.repository;

import org.netra.features.eligibility.entity.DeferralReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeferralReasonRepository extends JpaRepository<DeferralReason, UUID> {
    List<DeferralReason> findByRuleId(UUID ruleId);
}
