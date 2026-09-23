package org.netra.features.fulfillment.repository;

import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.fulfillment.entity.FulfillmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FulfillmentRepository extends JpaRepository<Fulfillment, UUID> {

    List<Fulfillment> findByBloodRequestId(UUID bloodRequestId);

    List<Fulfillment> findByDonationId(UUID donationId);

    @Query("SELECT f FROM Fulfillment f WHERE f.donationId = :donationId AND f.status NOT IN (org.netra.features.fulfillment.entity.FulfillmentStatus.CANCELLED, org.netra.features.fulfillment.entity.FulfillmentStatus.FAILED)")
    Optional<Fulfillment> findActiveByDonationId(@Param("donationId") UUID donationId);

    boolean existsByDonationIdAndStatusNotIn(UUID donationId, Collection<FulfillmentStatus> terminalStatuses);

    Page<Fulfillment> findByStatusInOrderByCreatedAtDesc(Collection<FulfillmentStatus> statuses, Pageable pageable);

    @Query("SELECT f FROM Fulfillment f WHERE f.status IN (org.netra.features.fulfillment.entity.FulfillmentStatus.READY, org.netra.features.fulfillment.entity.FulfillmentStatus.IN_PROGRESS) ORDER BY f.createdAt DESC")
    Page<Fulfillment> findPendingFulfillments(Pageable pageable);

    @Query("SELECT f FROM Fulfillment f WHERE f.createdByUserId = :userId " +
           "OR f.bloodRequestId IN (SELECT br.id FROM BloodRequest br WHERE br.requesterUserId = :userId) " +
           "OR f.donationId IN (SELECT d.id FROM Donation d WHERE d.donorUserId = :userId) " +
           "ORDER BY f.createdAt DESC")
    Page<Fulfillment> findMyFulfillments(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(f.units), 0) FROM Fulfillment f WHERE f.bloodRequestId = :bloodRequestId AND f.status IN (org.netra.features.fulfillment.entity.FulfillmentStatus.READY, org.netra.features.fulfillment.entity.FulfillmentStatus.IN_PROGRESS)")
    int sumReservedUnitsForBloodRequest(@Param("bloodRequestId") UUID bloodRequestId);

    @Query("SELECT COALESCE(SUM(f.units), 0) FROM Fulfillment f WHERE f.bloodRequestId = :bloodRequestId AND f.status = org.netra.features.fulfillment.entity.FulfillmentStatus.FULFILLED")
    int sumFulfilledUnitsForBloodRequest(@Param("bloodRequestId") UUID bloodRequestId);
}
