package com.team4.core.repositories;

import com.team4.core.entities.RoomImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {
    long countByRoomId(UUID roomId);

    List<RoomImage> findByRoomIdOrderByDisplayOrderAsc(UUID roomId);
}
