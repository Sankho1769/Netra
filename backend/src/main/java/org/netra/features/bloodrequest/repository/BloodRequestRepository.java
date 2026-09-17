package org.netra.features.bloodrequest.repository;

import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.donor.entity.BloodGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {

    Page<BloodRequest> findByRequesterUserId(UUID requesterUserId, Pageable pageable);

    Page<BloodRequest> findByRequesterUserIdAndStatus(UUID requesterUserId, BloodRequestStatus status, Pageable pageable);

    @Query("SELECT r FROM BloodRequest r WHERE r.status = :status " +
           "AND r.requiredBy > :now " +
           "AND (:bloodGroup IS NULL OR r.bloodGroup = :bloodGroup) " +
           "AND (:urgency IS NULL OR r.urgency = :urgency) " +
           "AND (:city IS NULL OR LOWER(r.city) = LOWER(:city))")
    Page<BloodRequest> findDiscoverableRequests(
            @Param("status") BloodRequestStatus status,
            @Param("now") Instant now,
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("urgency") BloodRequestUrgency urgency,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT r FROM BloodRequest r WHERE r.latitude BETWEEN :minLat AND :maxLat " +
           "AND r.longitude BETWEEN :minLng AND :maxLng AND r.status = :status " +
           "AND r.requiredBy > :now " +
           "AND (:bloodGroup IS NULL OR r.bloodGroup = :bloodGroup)")
    List<BloodRequest> findNearbyCandidates(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("status") BloodRequestStatus status,
            @Param("now") Instant now,
            @Param("bloodGroup") BloodGroup bloodGroup);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BloodRequest r SET r.status = org.netra.features.bloodrequest.entity.BloodRequestStatus.EXPIRED, " +
           "r.updatedAt = :now, r.version = r.version + 1 " +
           "WHERE r.status = org.netra.features.bloodrequest.entity.BloodRequestStatus.OPEN AND r.requiredBy <= :now")
    int expireDueRequests(@Param("now") Instant now);
}
