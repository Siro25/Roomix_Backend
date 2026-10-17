package com.team4.rental.repositories;

import com.team4.rental.entities.TenancyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenancyEventRepository extends JpaRepository<TenancyEvent, Long> {
}
