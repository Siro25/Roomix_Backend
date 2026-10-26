package com.team4.core.repositories;

import com.team4.core.entities.ProcessedRentalEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedRentalEventRepository extends JpaRepository<ProcessedRentalEvent, UUID> {
    Optional<ProcessedRentalEvent> findFirstByRoomIdOrderByEventSequenceDesc(UUID roomId);
}
