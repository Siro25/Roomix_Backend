package com.team4.rental.controllers;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.entities.TenancySettlement;
import com.team4.rental.services.TenancySettlementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenancies/{tenancyId}/settlement")
@RequiredArgsConstructor
public class TenancySettlementController {
    private final TenancySettlementService settlementService;

    @PostMapping("/preview")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<TenancySettlement> preview(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tenancyId, @Valid @RequestBody SettlementPreviewRequest request) {
        return ApiResponse.<TenancySettlement>builder().data(settlementService.preview(
                UUID.fromString(jwt.getSubject()), tenancyId, request)).build();
    }

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<TenancySettlement> complete(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tenancyId, @Valid @RequestBody CompleteSettlementRequest request) {
        return ApiResponse.<TenancySettlement>builder().data(settlementService.complete(
                UUID.fromString(jwt.getSubject()), tenancyId, request)).build();
    }

    @GetMapping
    public ApiResponse<TenancySettlement> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tenancyId) {
        return ApiResponse.<TenancySettlement>builder().data(settlementService.get(
                UUID.fromString(jwt.getSubject()), tenancyId)).build();
    }
}
