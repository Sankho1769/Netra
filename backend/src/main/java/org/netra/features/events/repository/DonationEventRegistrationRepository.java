package org.netra.features.events.repository;

import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationEventRegistrationRepository extends JpaRepository<DonationEventRegistration, UUID> {

    Optional<DonationEventRegistration> findByEventIdAndDonorUserId(UUID eventId, UUID donorUserId);

    boolean existsByEventIdAndDonorUserIdAndStatusIn(
            UUID eventId, UUID donorUserId, Collection<DonationEventRegistrationStatus> statuses);

    List<DonationEventRegistration> findByEventIdOrderByRegisteredAtDesc(UUID eventId);

    List<DonationEventRegistration> findByDonorUserIdOrderByRegisteredAtDesc(UUID donorUserId);

    @Query("SELECT COUNT(r) FROM DonationEventRegistration r WHERE r.eventId = :eventId AND r.status IN :statuses")
    long countSlotConsumingRegistrations(
            @Param("eventId") UUID eventId,
            @Param("statuses") Collection<DonationEventRegistrationStatus> statuses);
}
