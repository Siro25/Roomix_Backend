package com.team4.core.repositories;

import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.enums.PostStatus;
import com.team4.core.enums.RoomStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
        Page<Post> findByStatus(PostStatus status, Pageable pageable);

        long countByStatus(PostStatus status);

        Optional<Post> findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(UUID roomId, UUID landlordId);

        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        Page<Post> findByLandlordId(UUID landlordId, Pageable pageable);

        List<Post> findByLandlordIdOrderByCreatedAtDesc(UUID landlordId);

        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        List<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status);

        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        @Query("""
                        select post
                        from Post post
                        where post.room.floor.house.id = :houseId
                          and post.landlordId = :landlordId
                        order by post.createdAt desc
                        """)
        List<Post> findByHouseIdAndLandlordId(
                        @Param("houseId") UUID houseId,
                        @Param("landlordId") UUID landlordId);

        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        @Query("""
                        select post
                        from Post post
                        where post.room.floor.house.id = :houseId
                          and post.status = com.team4.core.enums.PostStatus.APPROVED
                        order by post.room.floor.floorNumber asc, post.room.roomNumber asc
                        """)
        List<Post> findPublicByHouseId(@Param("houseId") UUID houseId);

        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        Optional<Post> findFirstByRoomIdOrderByCreatedAtDesc(UUID roomId);

        @Override
        @EntityGraph(attributePaths = { "room", "room.floor", "room.floor.house" })
        Page<Post> findAll(Specification<Post> specification, Pageable pageable);

        default Page<Post> findPublicRooms(
                        Specification<Post> filters,
                        Pageable pageable) {
                Specification<Post> publicVisibility = (root, query, criteriaBuilder) -> {
                        Join<Post, Room> room = root.join("room");
                        Predicate approved = criteriaBuilder.equal(
                                        root.get("status"), PostStatus.APPROVED);
                        Predicate notRented = criteriaBuilder.isFalse(room.get("rented"));
                        Predicate available = criteriaBuilder.equal(room.get("status"), RoomStatus.AVAILABLE);
                        Predicate optionalFilters = filters.toPredicate(root, query, criteriaBuilder);
                        if (optionalFilters == null) {
                                return criteriaBuilder.and(approved, notRented, available);
                        }
                        return criteriaBuilder.and(approved, notRented, available, optionalFilters);
                };
                return findAll(publicVisibility, pageable);
        }
}
