package com.team4.rental.controllers;

import com.team4.rental.dtos.request.AcceptRentalRequest;
import com.team4.rental.dtos.request.ReasonRequest;
import com.team4.rental.dtos.request.SendRentalRequest;
import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.ContactCardResponse;
import com.team4.rental.dtos.response.RentalAcceptanceResponse;
import com.team4.rental.entities.RentalRequest;
import com.team4.rental.enums.RequestStatus;
import com.team4.rental.services.RentalRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalRequestController {
    private final RentalRequestService rentalRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<RentalRequest> send(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendRentalRequest request) {
        RentalRequest rentalRequest = rentalRequestService.send(
                UUID.fromString(jwt.getSubject()),
                jwt.getTokenValue(),
                request);

        return ApiResponse.<RentalRequest>builder()
                .message("Gửi yêu cầu thuê thành công")
                .data(rentalRequest)
                .build();
    }

    @GetMapping
    public ApiResponse<Page<RentalRequest>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending().and(Sort.by("id")));
        Page<RentalRequest> rentalRequests = rentalRequestService.list(
                UUID.fromString(jwt.getSubject()),
                status,
                roomId,
                pageable);

        return ApiResponse.<Page<RentalRequest>>builder()
                .data(rentalRequests)
                .build();
    }

    @GetMapping("/{requestId}")
    public ApiResponse<RentalRequest> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId) {
        RentalRequest rentalRequest = rentalRequestService.get(
                UUID.fromString(jwt.getSubject()),
                requestId);

        return ApiResponse.<RentalRequest>builder()
                .data(rentalRequest)
                .build();
    }

    @GetMapping("/{requestId}/contact-card")
    public ApiResponse<ContactCardResponse> getContactCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId) {
        ContactCardResponse contactCard = rentalRequestService.getContactCard(
                UUID.fromString(jwt.getSubject()),
                requestId);

        return ApiResponse.<ContactCardResponse>builder()
                .message("Lấy danh thiếp chủ trọ thành công")
                .data(contactCard)
                .build();
    }

    @PostMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<RentalRequest> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId) {
        RentalRequest rentalRequest = rentalRequestService.cancel(
                UUID.fromString(jwt.getSubject()),
                requestId);

        return ApiResponse.<RentalRequest>builder()
                .message("Hủy yêu cầu thuê thành công")
                .data(rentalRequest)
                .build();
    }

    @PostMapping("/{requestId}/accept")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<RentalAcceptanceResponse> accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @Valid @RequestBody AcceptRentalRequest request) {
        RentalAcceptanceResponse response = rentalRequestService.accept(
                UUID.fromString(jwt.getSubject()),
                jwt.getTokenValue(),
                requestId,
                request);

        return ApiResponse.<RentalAcceptanceResponse>builder()
                .message("Chấp nhận yêu cầu và gửi danh thiếp thành công")
                .data(response)
                .build();
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<RentalRequest> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @Valid @RequestBody ReasonRequest request) {
        RentalRequest rentalRequest = rentalRequestService.reject(
                UUID.fromString(jwt.getSubject()),
                requestId,
                request);

        return ApiResponse.<RentalRequest>builder()
                .message("Từ chối yêu cầu thuê thành công")
                .data(rentalRequest)
                .build();
    }
}
