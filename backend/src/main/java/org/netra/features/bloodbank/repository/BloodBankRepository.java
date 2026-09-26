package org.netra.features.bloodbank.repository;

import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, UUID> {

    Page<BloodBank> findByVerificationStatus(BloodBankVerificationStatus status, Pageable pageable);

    Page<BloodBank> findByCityIgnoreCaseAndVerificationStatus(String city, BloodBankVerificationStatus status, Pageable pageable);

    Page<BloodBank> findByOperatingStatusAndVerificationStatus(
            BloodBankOperatingStatus operatingStatus,
            BloodBankVerificationStatus verificationStatus,
            Pageable pageable
    );

    Page<BloodBank> findByCityIgnoreCaseAndOperatingStatusAndVerificationStatus(
            String city,
            BloodBankOperatingStatus operatingStatus,
            BloodBankVerificationStatus verificationStatus,
            Pageable pageable
    );

    List<BloodBank> findByVerificationStatus(BloodBankVerificationStatus status);

    @Query("SELECT b FROM BloodBank b WHERE b.latitude BETWEEN :minLat AND :maxLat AND b.longitude BETWEEN :minLng AND :maxLng AND b.verificationStatus = :status")
    List<BloodBank> findNearbyCandidates(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("status") BloodBankVerificationStatus status
    );

    @Query("SELECT b FROM BloodBank b WHERE b.verificationStatus = :status " +
           "AND (:operatingStatus IS NULL OR b.operatingStatus = :operatingStatus) " +
           "AND EXISTS (SELECT 1 FROM BloodInventory i WHERE i.bloodBankId = b.id AND i.bloodGroup = :bloodGroup AND i.unitsAvailable > 0)")
    Page<BloodBank> findAvailableByBloodGroup(
            @Param("bloodGroup") org.netra.features.donor.entity.BloodGroup bloodGroup,
            @Param("operatingStatus") BloodBankOperatingStatus operatingStatus,
            @Param("status") BloodBankVerificationStatus status,
            Pageable pageable
    );

    default Page<BloodBank> findAvailableByBloodGroup(
            org.netra.features.donor.entity.BloodGroup bloodGroup,
            BloodBankVerificationStatus status,
            Pageable pageable) {
        return findAvailableByBloodGroup(bloodGroup, null, status, pageable);
    }

    @Query("SELECT b FROM BloodBank b WHERE LOWER(b.city) = LOWER(:city) AND b.verificationStatus = :status " +
           "AND (:operatingStatus IS NULL OR b.operatingStatus = :operatingStatus) " +
           "AND EXISTS (SELECT 1 FROM BloodInventory i WHERE i.bloodBankId = b.id AND i.bloodGroup = :bloodGroup AND i.unitsAvailable > 0)")
    Page<BloodBank> findAvailableByCityAndBloodGroup(
            @Param("city") String city,
            @Param("bloodGroup") org.netra.features.donor.entity.BloodGroup bloodGroup,
            @Param("operatingStatus") BloodBankOperatingStatus operatingStatus,
            @Param("status") BloodBankVerificationStatus status,
            Pageable pageable
    );

    default Page<BloodBank> findAvailableByCityAndBloodGroup(
            String city,
            org.netra.features.donor.entity.BloodGroup bloodGroup,
            BloodBankVerificationStatus status,
            Pageable pageable) {
        return findAvailableByCityAndBloodGroup(city, bloodGroup, null, status, pageable);
    }
}
