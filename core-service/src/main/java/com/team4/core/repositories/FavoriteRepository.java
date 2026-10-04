package com.team4.core.repositories;

import com.team4.core.entities.Favorite;
import com.team4.core.enums.PostStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByTenant_IdAndPost_Id(UUID tenantId, UUID postId);

    List<Favorite> findAllByTenant_IdAndPost_RoomId(UUID tenantId, UUID roomId);

    @Query("""
            select distinct favorite
            from Favorite favorite
            join fetch favorite.post post
            join fetch post.room room
            join fetch room.floor floor
            join fetch floor.house
            where favorite.tenant.id = :tenantId
              and post.status = :status
            order by favorite.createdAt desc
            """)
    List<Favorite> findVisibleByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("status") PostStatus status);
}
