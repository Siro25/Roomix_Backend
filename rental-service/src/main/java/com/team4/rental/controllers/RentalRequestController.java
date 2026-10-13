package com.team4.rental.controllers;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.*;
import com.team4.rental.entities.*;
import com.team4.rental.services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalRequestController {
    private final RentalRequestService rentalRequestService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<RentalRequest> send(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody SendRentalRequest request) {
        return ApiResponse.<RentalRequest>builder().data(rentalRequestService.send(
                UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), request)).build();
    }

    @GetMapping
    public ApiResponse<Page<RentalRequest>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<Page<RentalRequest>>builder().data(rentalRequestService.list(
                UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"),
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))))).build();
    }

    @PostMapping("/{requestId}/accept")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Tenancy> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                       @Valid @RequestBody AcceptRentalRequest request) {
        return ApiResponse.<Tenancy>builder().data(rentalRequestService.accept(
                UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), requestId, request)).build();
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<RentalRequest> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId,
                                             @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.<RentalRequest>builder().data(rentalRequestService.reject(
                UUID.fromString(jwt.getSubject()), requestId, request)).build();
    }
}
