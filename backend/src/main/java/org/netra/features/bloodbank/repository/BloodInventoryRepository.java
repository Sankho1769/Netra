package org.netra.features.bloodbank.repository;

import org.netra.features.bloodbank.entity.BloodInventory;
import org.netra.features.donor.entity.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, UUID> {

    List<BloodInventory> findByBloodBankId(UUID bloodBankId);

    Optional<BloodInventory> findByBloodBankIdAndBloodGroup(UUID bloodBankId, BloodGroup bloodGroup);

    boolean existsByBloodBankIdAndBloodGroup(UUID bloodBankId, BloodGroup bloodGroup);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BloodInventory b SET b.unitsAvailable = :newUnits, b.lastUpdatedAt = :now, b.updatedAt = :now, b.version = b.version + 1 WHERE b.id = :id AND b.version = :version AND :newUnits >= 0")
    int updateUnitsWithOptimisticLock(
            @Param("id") UUID id,
            @Param("version") Long version,
            @Param("newUnits") Integer newUnits,
            @Param("now") Instant now
    );
}
