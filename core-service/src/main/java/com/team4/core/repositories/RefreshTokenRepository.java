package com.team4.core.repositories;

import com.team4.core.entities.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> lockByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.familyId = :familyId and token.revokedAt is null")
    void revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.userId = :userId and token.revokedAt is null")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
