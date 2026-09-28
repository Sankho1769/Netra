package org.netra.features.karma.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.netra.features.karma.entity.KarmaAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KarmaAccountRepository extends JpaRepository<KarmaAccount, UUID> {

    Optional<KarmaAccount> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "30000")})
    @Query("SELECT k FROM KarmaAccount k WHERE k.userId = :userId")
    Optional<KarmaAccount> findByUserIdForUpdate(@Param("userId") UUID userId);
}
