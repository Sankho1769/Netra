package org.netra.features.donor.repository;

import org.netra.features.donor.entity.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, UUID> {

    Optional<DonorProfile> findByUserId(UUID userId);

    List<DonorProfile> findAllByUserIdIn(java.util.Collection<UUID> userIds);

    boolean existsByUserId(UUID userId);
}
