package com.team4.rental.repositories;

import com.team4.rental.entities.ActiveTenancy;
import com.team4.rental.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ActiveTenancyRepository extends JpaRepository<ActiveTenancy, UUID> {
    boolean existsByRoomId(UUID roomId);
    void deleteByTenancyId(UUID tenancyId);
}
