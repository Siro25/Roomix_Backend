package com.team4.rental.services;

import com.team4.rental.dtos.request.AcceptRentalRequest;
import com.team4.rental.dtos.request.ReasonRequest;
import com.team4.rental.dtos.request.SendRentalRequest;
import com.team4.rental.dtos.response.ContactCardResponse;
import com.team4.rental.dtos.response.RentalAcceptanceResponse;
import com.team4.rental.entities.RentalRequest;
import com.team4.rental.enums.RequestStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalRequestService {
    RentalRequest send(UUID tenantId, String token, SendRentalRequest request);

    Page<RentalRequest> list(UUID userId, RequestStatus status, UUID roomId, Pageable pageable);

    RentalRequest get(UUID userId, UUID requestId);

    RentalRequest cancel(UUID tenantId, String token, UUID requestId);

    RentalAcceptanceResponse accept(
            UUID landlordId,
            String token,
            UUID requestId,
            AcceptRentalRequest request);

    RentalRequest reject(
            UUID landlordId,
            String token,
            UUID requestId,
            ReasonRequest request);

    ContactCardResponse getContactCard(UUID userId, UUID requestId);
}
