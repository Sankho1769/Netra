package org.netra.features.matching.repository;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface DonorMatchingRepository extends JpaRepository<DonorProfile, UUID> {

    @Query("SELECT dp.id as donorProfileId, dp.userId as userId, u.fullName as fullName, " +
           "dp.bloodGroup as bloodGroup, dp.bloodGroupVerificationStatus as bloodGroupVerificationStatus, " +
           "dp.availabilityStatus as availabilityStatus, dp.donorStatus as donorStatus, " +
           "dp.lastDonationDate as lastDonationDate, dp.latitude as latitude, dp.longitude as longitude, " +
           "dp.createdAt as createdAt " +
           "FROM DonorProfile dp, User u " +
           "WHERE dp.userId = u.id " +
           "AND u.status = org.netra.features.user.entity.UserStatus.ACTIVE " +
           "AND dp.donorStatus = org.netra.features.donor.entity.DonorStatus.ACTIVE " +
           "AND dp.availabilityStatus = org.netra.features.donor.entity.DonorAvailabilityStatus.AVAILABLE " +
           "AND dp.bloodGroup IN :compatibleGroups " +
           "AND dp.userId != :requesterUserId " +
           "AND dp.bloodGroupVerificationStatus = org.netra.features.donor.entity.BloodGroupVerificationStatus.VERIFIED " +
           "AND dp.latitude IS NOT NULL AND dp.longitude IS NOT NULL " +
           "AND dp.latitude BETWEEN :minLat AND :maxLat " +
           "AND dp.longitude BETWEEN :minLng AND :maxLng " +
           "AND (dp.lastDonationDate IS NULL OR dp.lastDonationDate <= :maxLastDonationDate)")
    List<DonorCandidateProjection> findCandidateDonors(
            @Param("compatibleGroups") Collection<BloodGroup> compatibleGroups,
            @Param("requesterUserId") UUID requesterUserId,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("maxLastDonationDate") LocalDate maxLastDonationDate);
}
