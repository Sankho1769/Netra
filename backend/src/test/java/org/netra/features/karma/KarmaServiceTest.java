package org.netra.features.karma;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.features.karma.dto.KarmaSummaryDto;
import org.netra.features.karma.entity.KarmaAccount;
import org.netra.features.karma.entity.KarmaEventType;
import org.netra.features.karma.entity.KarmaTransaction;
import org.netra.features.karma.repository.KarmaAccountRepository;
import org.netra.features.karma.repository.KarmaTransactionRepository;
import org.netra.features.karma.service.KarmaService;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KarmaServiceTest {

    @Mock
    private KarmaAccountRepository karmaAccountRepository;

    @Mock
    private KarmaTransactionRepository karmaTransactionRepository;

    private KarmaService karmaService;

    private final UUID userId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        karmaService = new KarmaService(karmaAccountRepository, karmaTransactionRepository);
    }

    @Test
    @DisplayName("awardKarma: successfully increments balance and creates transaction")
    void awardKarma_Success() {
        KarmaAccount account = new KarmaAccount(userId);
        account.setBalance(100);

        when(karmaAccountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(karmaTransactionRepository.findByReferenceTypeAndReferenceIdAndEventType(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(karmaAccountRepository.save(any(KarmaAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(karmaTransactionRepository.save(any(KarmaTransaction.class))).thenAnswer(i -> i.getArgument(0));

        KarmaTransaction tx = karmaService.awardKarma(
                userId,
                KarmaEventType.VERIFIED_DONATION_COMPLETED,
                50,
                "DONATION",
                "donation-123",
                "Verified donation",
                actorId
        );

        assertNotNull(tx);
        assertEquals(50, tx.getPoints());
        assertEquals(100, tx.getPreviousBalance());
        assertEquals(150, tx.getResultingBalance());
        assertEquals(150, account.getBalance());
        assertEquals(KarmaEventType.VERIFIED_DONATION_COMPLETED, tx.getEventType());
        verify(karmaAccountRepository).save(account);
        verify(karmaTransactionRepository).save(any(KarmaTransaction.class));
    }

    @Test
    @DisplayName("awardKarma: idempotent skipping when transaction already exists")
    void awardKarma_Idempotent() {
        KarmaTransaction existingTx = new KarmaTransaction(
                UUID.randomUUID(), userId, 50, KarmaEventType.VERIFIED_DONATION_COMPLETED,
                "DONATION", "donation-123", "Verified donation", actorId, 100, 150
        );

        when(karmaTransactionRepository.findByReferenceTypeAndReferenceIdAndEventType(
                "DONATION", "donation-123", KarmaEventType.VERIFIED_DONATION_COMPLETED))
                .thenReturn(Optional.of(existingTx));

        KarmaTransaction tx = karmaService.awardKarma(
                userId,
                KarmaEventType.VERIFIED_DONATION_COMPLETED,
                50,
                "DONATION",
                "donation-123",
                "Duplicate call",
                actorId
        );

        assertSame(existingTx, tx);
        verify(karmaAccountRepository, never()).findByUserIdForUpdate(any());
        verify(karmaAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("awardKarma: throws IllegalArgumentException when points <= 0")
    void awardKarma_InvalidPoints() {
        assertThrows(IllegalArgumentException.class, () -> karmaService.awardKarma(
                userId, KarmaEventType.VERIFIED_DONATION_COMPLETED, 0, "DONATION", "id", "reason", actorId
        ));
        assertThrows(IllegalArgumentException.class, () -> karmaService.awardKarma(
                userId, KarmaEventType.VERIFIED_DONATION_COMPLETED, -10, "DONATION", "id", "reason", actorId
        ));
    }

    @Test
    @DisplayName("penalizeKarma: successfully deducts points and records negative transaction")
    void penalizeKarma_Success() {
        KarmaAccount account = new KarmaAccount(userId);
        account.setBalance(100);

        when(karmaAccountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(karmaTransactionRepository.findByReferenceTypeAndReferenceIdAndEventType(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(karmaAccountRepository.save(any(KarmaAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(karmaTransactionRepository.save(any(KarmaTransaction.class))).thenAnswer(i -> i.getArgument(0));

        KarmaTransaction tx = karmaService.penalizeKarma(
                userId,
                KarmaEventType.DONOR_NO_SHOW_CONFIRMED,
                30,
                "DONOR_MATCH",
                "match-456",
                "Confirmed no-show",
                actorId
        );

        assertNotNull(tx);
        assertEquals(-30, tx.getPoints());
        assertEquals(100, tx.getPreviousBalance());
        assertEquals(70, tx.getResultingBalance());
        assertEquals(70, account.getBalance());
        assertEquals(KarmaEventType.DONOR_NO_SHOW_CONFIRMED, tx.getEventType());
    }

    @Test
    @DisplayName("reverseTransaction: creates compensating reversal with inverse points")
    void reverseTransaction_Success() {
        UUID originalTxId = UUID.randomUUID();
        KarmaTransaction originalTx = new KarmaTransaction(
                UUID.randomUUID(), userId, -30, KarmaEventType.DONOR_NO_SHOW_CONFIRMED,
                "DONOR_MATCH", "match-456", "Penalty", actorId, 100, 70
        );
        originalTx.setId(originalTxId);

        KarmaAccount account = new KarmaAccount(userId);
        account.setBalance(70);

        when(karmaTransactionRepository.findById(originalTxId)).thenReturn(Optional.of(originalTx));
        when(karmaTransactionRepository.findByReferenceTypeAndReferenceIdAndEventType(
                "KARMA_TRANSACTION", originalTxId.toString(), KarmaEventType.COMPENSATING_REVERSAL))
                .thenReturn(Optional.empty());
        when(karmaAccountRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(account));
        when(karmaAccountRepository.save(any(KarmaAccount.class))).thenAnswer(i -> i.getArgument(0));
        when(karmaTransactionRepository.save(any(KarmaTransaction.class))).thenAnswer(i -> i.getArgument(0));

        KarmaTransaction reversalTx = karmaService.reverseTransaction(
                originalTxId, "Dispute upheld - donor was deferred medically", actorId);

        assertNotNull(reversalTx);
        assertEquals(30, reversalTx.getPoints()); // Inverse of -30 is +30
        assertEquals(70, reversalTx.getPreviousBalance());
        assertEquals(100, reversalTx.getResultingBalance());
        assertEquals(100, account.getBalance());
        assertEquals(KarmaEventType.COMPENSATING_REVERSAL, reversalTx.getEventType());
    }

    @Test
    @DisplayName("getKarmaSummary: correctly maps balance and reputation tiers")
    void getKarmaSummary_Tiers() {
        KarmaAccount account = new KarmaAccount(userId);
        account.setBalance(250);

        when(karmaAccountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(karmaTransactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Collections.emptyList());
        when(karmaTransactionRepository.countByUserId(userId)).thenReturn(5L);

        KarmaSummaryDto summary = karmaService.getKarmaSummary(userId);

        assertNotNull(summary);
        assertEquals(250, summary.getBalance());
        assertEquals("Gold Guardian", summary.getTier());
        assertEquals(5L, summary.getTotalTransactions());
    }
}
