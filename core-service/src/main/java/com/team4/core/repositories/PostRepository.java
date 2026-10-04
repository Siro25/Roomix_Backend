package com.team4.core.repositories;

import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.enums.PostStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Optional<Post> findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(UUID roomId, UUID landlordId);

    @EntityGraph(attributePaths = {"room", "room.floor", "room.floor.house"})
    Optional<Post> findFirstByRoomIdOrderByCreatedAtDesc(UUID roomId);

    @Override
    @EntityGraph(attributePaths = {"room", "room.floor", "room.floor.house"})
    Page<Post> findAll(Specification<Post> specification, Pageable pageable);

    default Page<Post> findPublicRooms(
            Specification<Post> filters,
            Pageable pageable) {
        Specification<Post> publicVisibility = (root, query, criteriaBuilder) -> {
            Join<Post, Room> room = root.join("room");
            Predicate approved = criteriaBuilder.equal(
                    root.get("status"), PostStatus.APPROVED);
            Predicate notRented = criteriaBuilder.isFalse(room.get("rented"));
            Predicate optionalFilters = filters.toPredicate(root, query, criteriaBuilder);
            if (optionalFilters == null) {
                return criteriaBuilder.and(approved, notRented);
            }
            return criteriaBuilder.and(approved, notRented, optionalFilters);
        };
        return findAll(publicVisibility, pageable);
    }
}
