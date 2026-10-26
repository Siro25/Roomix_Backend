package com.team4.rental.repositories;

import com.team4.rental.entities.MeterReading;
import com.team4.rental.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    Optional<MeterReading> findFirstByTenancyIdOrderByBillingMonthDesc(UUID tenancyId);
}
