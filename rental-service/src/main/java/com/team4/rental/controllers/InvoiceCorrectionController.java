package com.team4.rental.controllers;

import com.team4.rental.dtos.request.CorrectInvoiceRequest;
import com.team4.rental.dtos.response.*;
import com.team4.rental.entities.InvoiceCorrection;
import com.team4.rental.services.InvoiceCorrectionService;
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
@RequestMapping("/api/invoices/{invoiceId}/corrections")
@RequiredArgsConstructor
public class InvoiceCorrectionController {
    private final InvoiceCorrectionService correctionService;

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<InvoiceDetailResponse> correct(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invoiceId, @Valid @RequestBody CorrectInvoiceRequest request) {
        return ApiResponse.<InvoiceDetailResponse>builder().data(correctionService.correct(
                UUID.fromString(jwt.getSubject()), invoiceId, request)).build();
    }

    @GetMapping
    public ApiResponse<Page<InvoiceCorrection>> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invoiceId, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<Page<InvoiceCorrection>>builder().data(correctionService.history(
                UUID.fromString(jwt.getSubject()), invoiceId, PageRequest.of(page, size))).build();
    }
}
