package org.netra.features.auth.repository;

import org.netra.features.auth.entity.EmailVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, UUID> {

    Optional<EmailVerificationChallenge> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
