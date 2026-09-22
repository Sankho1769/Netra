package org.netra.features.donation.repository;

import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByDonorUserIdOrderByDonationDateDesc(UUID donorUserId, Pageable pageable);

    List<Donation> findByDonorUserIdOrderByDonationDateDesc(UUID donorUserId);

    Page<Donation> findByVerificationStatusOrderByCreatedAtDesc(DonationVerificationStatus status, Pageable pageable);

    List<Donation> findByVerificationStatusOrderByCreatedAtDesc(DonationVerificationStatus status);

    Optional<Donation> findByDonorUserIdAndBloodRequestId(UUID donorUserId, UUID bloodRequestId);

    Optional<Donation> findByDonorUserIdAndDonationEventId(UUID donorUserId, UUID donationEventId);

    boolean existsByDonorUserIdAndBloodRequestIdAndVerificationStatusIn(
            UUID donorUserId, UUID bloodRequestId, Collection<DonationVerificationStatus> statuses);

    boolean existsByDonorUserIdAndDonationEventIdAndVerificationStatusIn(
            UUID donorUserId, UUID donationEventId, Collection<DonationVerificationStatus> statuses);

    @Query("SELECT MAX(d.donationDate) FROM Donation d WHERE d.donorUserId = :donorUserId AND d.verificationStatus = org.netra.features.donation.entity.DonationVerificationStatus.VERIFIED")
    Optional<LocalDate> findLatestVerifiedDonationDate(@Param("donorUserId") UUID donorUserId);

    long countByDonorUserIdAndVerificationStatus(UUID donorUserId, DonationVerificationStatus status);
}
