package com.team4.rental.services;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.TenancySettlement;
import java.util.UUID;

public interface TenancySettlementService {
    TenancySettlement preview(UUID landlordId, UUID tenancyId, SettlementPreviewRequest request);
    TenancySettlement complete(
            UUID landlordId,
            String token,
            UUID tenancyId,
            CompleteSettlementRequest request);
    TenancySettlement get(UUID userId, UUID tenancyId);
}
