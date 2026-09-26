package org.netra.features.bloodbank.repository;

import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodBankAccountRepository extends JpaRepository<BloodBankAccount, UUID> {

    Optional<BloodBankAccount> findByUserIdAndBloodBankId(UUID userId, UUID bloodBankId);

    boolean existsByUserIdAndBloodBankIdAndStatus(UUID userId, UUID bloodBankId, BloodBankAccountStatus status);

    boolean existsByUserIdAndStatus(UUID userId, BloodBankAccountStatus status);

    List<BloodBankAccount> findByBloodBankId(UUID bloodBankId);

    List<BloodBankAccount> findByUserId(UUID userId);
}
