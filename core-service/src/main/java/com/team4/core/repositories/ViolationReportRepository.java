package com.team4.core.repositories;

import com.team4.core.entities.ViolationReport;
import com.team4.core.enums.ReportStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationReportRepository extends JpaRepository<ViolationReport, UUID> {
    Page<ViolationReport> findByReporterId(UUID reporterId, Pageable pageable);

    Page<ViolationReport> findByStatus(ReportStatus status, Pageable pageable);
}
