package com.team4.rental.services;

import com.team4.rental.dtos.request.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.RequestStatus;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface RentalRequestService {
    RentalRequest send(UUID tenantId, String token, SendRentalRequest request);
    Page<RentalRequest> list(UUID userId, RequestStatus status, UUID roomId, Pageable pageable);
    RentalRequest get(UUID userId, UUID requestId);
    RentalRequest cancel(UUID tenantId, UUID requestId);
    Tenancy accept(UUID landlordId, String token, UUID requestId, AcceptRentalRequest request);
    RentalRequest reject(UUID landlordId, UUID requestId, ReasonRequest request);
}
