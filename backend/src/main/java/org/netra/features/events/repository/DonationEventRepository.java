package org.netra.features.events.repository;

import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DonationEventRepository extends JpaRepository<DonationEvent, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonationEvent e SET e.currentRegistrationCount = e.currentRegistrationCount + 1, e.updatedAt = :now " +
           "WHERE e.id = :eventId AND e.currentRegistrationCount < e.donorCapacity AND e.status = 'PUBLISHED'")
    int acquireRegistrationSlot(@Param("eventId") UUID eventId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonationEvent e SET e.currentRegistrationCount = e.currentRegistrationCount - 1, e.updatedAt = :now " +
           "WHERE e.id = :eventId AND e.currentRegistrationCount > 0")
    int releaseRegistrationSlot(@Param("eventId") UUID eventId, @Param("now") Instant now);

    @Query("SELECT e FROM DonationEvent e WHERE e.latitude BETWEEN :minLat AND :maxLat " +
           "AND e.longitude BETWEEN :minLng AND :maxLng AND e.status = :status")
    List<DonationEvent> findNearbyCandidates(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("status") DonationEventStatus status);

    Page<DonationEvent> findByStatusIn(Collection<DonationEventStatus> statuses, Pageable pageable);

    Page<DonationEvent> findByCityIgnoreCaseAndStatusIn(String city, Collection<DonationEventStatus> statuses, Pageable pageable);

    Page<DonationEvent> findByBloodBankId(UUID bloodBankId, Pageable pageable);

    Page<DonationEvent> findByBloodBankIdAndStatus(UUID bloodBankId, DonationEventStatus status, Pageable pageable);

    Page<DonationEvent> findByBloodBankIdAndStatusIn(UUID bloodBankId, Collection<DonationEventStatus> statuses, Pageable pageable);

    @Query("SELECT e FROM DonationEvent e WHERE e.bloodBankId = :bloodBankId AND e.status IN :statuses AND e.endAt >= :now")
    Page<DonationEvent> findUpcomingEventsByBloodBankId(
            @Param("bloodBankId") UUID bloodBankId,
            @Param("statuses") Collection<DonationEventStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT e FROM DonationEvent e WHERE e.status IN :statuses AND e.endAt >= :now")
    Page<DonationEvent> findUpcomingEvents(
            @Param("statuses") Collection<DonationEventStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT e FROM DonationEvent e WHERE LOWER(e.city) = LOWER(:city) AND e.status IN :statuses AND e.endAt >= :now")
    Page<DonationEvent> findUpcomingEventsByCity(
            @Param("city") String city,
            @Param("statuses") Collection<DonationEventStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonationEvent e SET e.status = org.netra.features.events.entity.DonationEventStatus.REGISTRATION_CLOSED, e.updatedAt = :now " +
           "WHERE e.status = org.netra.features.events.entity.DonationEventStatus.PUBLISHED AND e.registrationCloseAt <= :now")
    int closeDueRegistrations(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonationEvent e SET e.status = org.netra.features.events.entity.DonationEventStatus.ONGOING, e.updatedAt = :now " +
           "WHERE e.status = org.netra.features.events.entity.DonationEventStatus.REGISTRATION_CLOSED AND e.startAt <= :now")
    int startDueEvents(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonationEvent e SET e.status = org.netra.features.events.entity.DonationEventStatus.COMPLETED, e.updatedAt = :now " +
           "WHERE e.status = org.netra.features.events.entity.DonationEventStatus.ONGOING AND e.endAt <= :now")
    int completeDueEvents(@Param("now") Instant now);
}
