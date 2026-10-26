package com.team4.core.repositories;

import com.team4.core.entities.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> lockByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken token set token.usedAt = :usedAt "
            + "where token.userId = :userId and token.usedAt is null")
    void invalidateActiveByUserId(UUID userId, Instant usedAt);
}
