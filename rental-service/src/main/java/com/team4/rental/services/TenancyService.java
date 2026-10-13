package com.team4.rental.services;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.Tenancy;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface TenancyService {
    Page<Tenancy> list(UUID userId, String role, Pageable pageable);
    Tenancy get(UUID userId, UUID tenancyId);
    Tenancy confirmDeposit(UUID landlordId, UUID tenancyId);
    Tenancy cancel(UUID userId, UUID tenancyId, ReasonRequest request);
    Tenancy end(UUID landlordId, UUID tenancyId, EndTenancyRequest request);
}
