package com.team4.core.repositories;

import com.team4.core.entities.Post;
import com.team4.core.enums.PostStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
}
