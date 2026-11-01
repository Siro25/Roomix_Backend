package com.team4.rental.controllers;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.TenancyStatus;
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
@RequestMapping("/api/tenancies")
@RequiredArgsConstructor
public class TenancyController {
    private final TenancyService tenancyService;

    @GetMapping
    public ApiResponse<Page<Tenancy>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TenancyStatus status,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<Page<Tenancy>>builder().data(tenancyService.list(
                UUID.fromString(jwt.getSubject()), status, roomId,
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))))).build();
    }

    @GetMapping("/{tenancyId}")
    public ApiResponse<Tenancy> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tenancyId) {
        return ApiResponse.<Tenancy>builder().data(tenancyService.get(
                UUID.fromString(jwt.getSubject()), tenancyId)).build();
    }

    @PostMapping("/{tenancyId}/confirm-deposit")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Tenancy> confirmDeposit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tenancyId) {
        return ApiResponse.<Tenancy>builder().data(tenancyService.confirmDeposit(
                UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), tenancyId)).build();
    }

    @PostMapping("/{tenancyId}/cancel")
    public ApiResponse<Tenancy> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tenancyId,
                                       @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.<Tenancy>builder().data(tenancyService.cancel(
                UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), tenancyId, request)).build();
    }

    @PostMapping("/{tenancyId}/end")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Tenancy> end(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tenancyId,
                                    @Valid @RequestBody EndTenancyRequest request) {
        return ApiResponse.<Tenancy>builder().data(tenancyService.end(
                UUID.fromString(jwt.getSubject()), jwt.getTokenValue(), tenancyId, request)).build();
    }
}
