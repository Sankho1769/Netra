package org.netra.features.hospital.repository;

import org.netra.features.hospital.entity.VerifiedHospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerifiedHospitalRepository extends JpaRepository<VerifiedHospital, UUID> {

    Optional<VerifiedHospital> findByPlaceId(String placeId);

    Optional<VerifiedHospital> findByNameIgnoreCase(String name);

    Optional<VerifiedHospital> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    @Query("SELECT h FROM VerifiedHospital h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(h.address) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(h.city) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:city IS NULL OR LOWER(h.city) = LOWER(:city))")
    List<VerifiedHospital> searchHospitals(@Param("query") String query, @Param("city") String city);

    long countByVerificationStatus(String verificationStatus);
}
