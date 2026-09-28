package org.netra.features.karma.service;

import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.observability.StructuredLogger;
import org.netra.features.karma.dto.KarmaSummaryDto;
import org.netra.features.karma.dto.KarmaTransactionDto;
import org.netra.features.karma.entity.KarmaAccount;
import org.netra.features.karma.entity.KarmaEventType;
import org.netra.features.karma.entity.KarmaTransaction;
import org.netra.features.karma.repository.KarmaAccountRepository;
import org.netra.features.karma.repository.KarmaTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KarmaService {

    private static final Logger log = LoggerFactory.getLogger(KarmaService.class);

    private final KarmaAccountRepository karmaAccountRepository;
    private final KarmaTransactionRepository karmaTransactionRepository;

    public KarmaService(
            KarmaAccountRepository karmaAccountRepository,
            KarmaTransactionRepository karmaTransactionRepository) {
        this.karmaAccountRepository = karmaAccountRepository;
        this.karmaTransactionRepository = karmaTransactionRepository;
    }

    /**
     * Atomically and idempotently awards positive Karma points to a user.
     */
    @Transactional
    public KarmaTransaction awardKarma(
            UUID userId,
            KarmaEventType eventType,
            int points,
            String referenceType,
            String referenceId,
            String reason,
            UUID actorId) {

        if (points <= 0) {
            throw new IllegalArgumentException("Awarded karma points must be strictly positive (> 0).");
        }

        // Idempotency check: prevent duplicate awards for the same business event
        if (referenceType != null && referenceId != null) {
            Optional<KarmaTransaction> existing = karmaTransactionRepository
                    .findByReferenceTypeAndReferenceIdAndEventType(referenceType, referenceId, eventType);
            if (existing.isPresent()) {
                log.info("Karma award already recorded for refType={}, refId={}, eventType={}. Skipping duplicate.",
                        referenceType, referenceId, eventType);
                return existing.get();
            }
        }

        KarmaAccount account = getOrCreateAccountForUpdate(userId);
        int prevBalance = account.getBalance();
        int resultingBalance = prevBalance + points;

        account.setBalance(resultingBalance);
        account.setUpdatedAt(Instant.now());
        karmaAccountRepository.save(account);

        KarmaTransaction tx = new KarmaTransaction(
                account.getId(),
                userId,
                points,
                eventType,
                referenceType,
                referenceId,
                reason,
                actorId,
                prevBalance,
                resultingBalance
        );
        tx = karmaTransactionRepository.save(tx);

        StructuredLogger.logOperation(
                "KARMA_AWARDED",
                userId,
                null,
                "KarmaAccount",
                account.getId(),
                "AWARD",
                null,
                "SUCCESS"
        );

        return tx;
    }

    /**
     * Atomically and idempotently deducts Karma points following an authoritative penalty decision.
     */
    @Transactional
    public KarmaTransaction penalizeKarma(
            UUID userId,
            KarmaEventType eventType,
            int points,
            String referenceType,
            String referenceId,
            String reason,
            UUID actorId) {

        if (points <= 0) {
            throw new IllegalArgumentException("Penalty karma points must be strictly positive (> 0).");
        }

        // Idempotency check
        if (referenceType != null && referenceId != null) {
            Optional<KarmaTransaction> existing = karmaTransactionRepository
                    .findByReferenceTypeAndReferenceIdAndEventType(referenceType, referenceId, eventType);
            if (existing.isPresent()) {
                log.info("Karma penalty already recorded for refType={}, refId={}, eventType={}. Skipping duplicate.",
                        referenceType, referenceId, eventType);
                return existing.get();
            }
        }

        KarmaAccount account = getOrCreateAccountForUpdate(userId);
        int prevBalance = account.getBalance();
        int resultingBalance = prevBalance - points;

        account.setBalance(resultingBalance);
        account.setUpdatedAt(Instant.now());
        karmaAccountRepository.save(account);

        KarmaTransaction tx = new KarmaTransaction(
                account.getId(),
                userId,
                -points,
                eventType,
                referenceType,
                referenceId,
                reason,
                actorId,
                prevBalance,
                resultingBalance
        );
        tx = karmaTransactionRepository.save(tx);

        StructuredLogger.logOperation(
                "KARMA_PENALIZED",
                userId,
                null,
                "KarmaAccount",
                account.getId(),
                "PENALIZE",
                null,
                "SUCCESS"
        );

        return tx;
    }

    /**
     * Reverses a previous transaction (award or penalty) with an auditable compensating transaction.
     */
    @Transactional
    public KarmaTransaction reverseTransaction(UUID originalTransactionId, String reason, UUID adminActorId) {
        KarmaTransaction original = karmaTransactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Karma transaction not found with ID: " + originalTransactionId));

        int reversalPoints = -original.getPoints();
        String refId = original.getId().toString();

        Optional<KarmaTransaction> existingReversal = karmaTransactionRepository
                .findByReferenceTypeAndReferenceIdAndEventType("KARMA_TRANSACTION", refId, KarmaEventType.COMPENSATING_REVERSAL);
        if (existingReversal.isPresent()) {
            return existingReversal.get();
        }

        KarmaAccount account = getOrCreateAccountForUpdate(original.getUserId());
        int prevBalance = account.getBalance();
        int resultingBalance = prevBalance + reversalPoints;

        account.setBalance(resultingBalance);
        account.setUpdatedAt(Instant.now());
        karmaAccountRepository.save(account);

        KarmaTransaction reversalTx = new KarmaTransaction(
                account.getId(),
                original.getUserId(),
                reversalPoints,
                KarmaEventType.COMPENSATING_REVERSAL,
                "KARMA_TRANSACTION",
                refId,
                "Reversal of transaction " + original.getId() + ": " + reason,
                adminActorId,
                prevBalance,
                resultingBalance
        );

        return karmaTransactionRepository.save(reversalTx);
    }

    /**
     * Retrieves the current user's Karma balance, reputation tier, and recent transaction history.
     */
    @Transactional(readOnly = true)
    public KarmaSummaryDto getKarmaSummary(UUID userId) {
        KarmaAccount account = karmaAccountRepository.findByUserId(userId)
                .orElseGet(() -> new KarmaAccount(userId));

        List<KarmaTransaction> recent = karmaTransactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        long totalCount = karmaTransactionRepository.countByUserId(userId);

        List<KarmaTransactionDto> recentDtos = recent.stream()
                .map(KarmaTransactionDto::fromEntity)
                .collect(Collectors.toList());

        return new KarmaSummaryDto(
                userId,
                account.getBalance(),
                KarmaSummaryDto.computeTier(account.getBalance()),
                totalCount,
                recentDtos
        );
    }

    /**
     * Retrieves paginated transaction history for an authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<KarmaTransactionDto> getTransactionHistory(UUID userId, Pageable pageable) {
        return karmaTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(KarmaTransactionDto::fromEntity);
    }

    private KarmaAccount getOrCreateAccountForUpdate(UUID userId) {
        return karmaAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    KarmaAccount newAcc = new KarmaAccount(userId);
                    return karmaAccountRepository.save(newAcc);
                });
    }
}
