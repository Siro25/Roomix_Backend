package com.team4.core.repositories;

import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
            SELECT user
            FROM User user
            WHERE user.role = :role
              AND (:status IS NULL OR user.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(user.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR user.phoneNumber LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<User> searchAdminUsers(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
