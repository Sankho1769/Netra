package org.netra.features.notification.repository;

import org.netra.features.notification.entity.UserDeviceToken;
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
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    List<UserDeviceToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<UserDeviceToken> findByToken(String token);

    Optional<UserDeviceToken> findByTokenHash(String tokenHash);

    Optional<UserDeviceToken> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false, t.revokedAt = :now, t.updatedAt = :now WHERE t.token = :token")
    int deactivateToken(@Param("token") String token, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserDeviceToken t SET t.active = false, t.revokedAt = :now, t.updatedAt = :now WHERE t.id = :id AND t.userId = :userId")
    int deactivateTokenByIdAndUser(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") Instant now);
}
