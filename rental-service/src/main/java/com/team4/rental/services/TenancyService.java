package com.team4.rental.services;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.Tenancy;
import com.team4.rental.enums.TenancyStatus;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface TenancyService {
    Page<Tenancy> list(UUID userId, TenancyStatus status, UUID roomId, Pageable pageable);
    Tenancy get(UUID userId, UUID tenancyId);
    Tenancy confirmDeposit(UUID landlordId, String token, UUID tenancyId);
    Tenancy cancel(UUID userId, String token, UUID tenancyId, ReasonRequest request);
    Tenancy end(UUID landlordId, String token, UUID tenancyId, EndTenancyRequest request);
}
