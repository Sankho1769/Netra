package org.netra.features.matching.repository;

import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.donor.entity.BloodGroupVerificationStatus;
import org.netra.features.donor.entity.DonorAvailabilityStatus;
import org.netra.features.donor.entity.DonorStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Projection interface for candidate donors, joining donor_profiles and users
 * to avoid N+1 queries.
 */
public interface DonorCandidateProjection {

    UUID getDonorProfileId();

    UUID getUserId();

    String getFullName();

    BloodGroup getBloodGroup();

    BloodGroupVerificationStatus getBloodGroupVerificationStatus();

    DonorAvailabilityStatus getAvailabilityStatus();

    DonorStatus getDonorStatus();

    LocalDate getLastDonationDate();

    String getBiologicalSex();

    Double getLatitude();

    Double getLongitude();

    Instant getCreatedAt();
}
