package com.example.ecommerce_api.user.repository;

import com.example.ecommerce_api.user.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Locks the row for the duration of the transaction so that two concurrent
     * refresh requests presenting the same token are serialized: the loser sees
     * the token already revoked and is treated as a reuse attempt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByDevice_IdAndRevokedFalseAndExpiresAtAfter(Long deviceId, Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken rt set rt.revoked = true, rt.revokedAt = :now " +
            "where rt.device.id = :deviceId and rt.revoked = false")
    int revokeAllForDevice(@Param("deviceId") Long deviceId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken rt set rt.revoked = true, rt.revokedAt = :now " +
            "where rt.device.user.id = :userId and rt.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant time);
}
