package org.netra.features.eligibility.repository;

import org.netra.features.eligibility.entity.EligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, UUID> {
    
    @Query("SELECT r FROM EligibilityRule r LEFT JOIN FETCH r.deferralReasons " +
           "WHERE r.version = :version AND r.active = true " +
           "AND r.effectiveFrom <= :now AND (r.effectiveTo IS NULL OR r.effectiveTo >= :now)")
    List<EligibilityRule> findActiveRulesByVersion(@Param("version") String version, @Param("now") Instant now);

    Optional<EligibilityRule> findByVersionAndRuleId(String version, String ruleId);
}
