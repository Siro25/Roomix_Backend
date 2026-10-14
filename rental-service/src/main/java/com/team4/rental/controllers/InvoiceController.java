package com.team4.rental.controllers;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.*;
import com.team4.rental.entities.*;
import com.team4.rental.enums.InvoiceStatus;
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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<InvoiceDetailResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody CreateInvoiceRequest request) {
        return ApiResponse.<InvoiceDetailResponse>builder().data(invoiceService.create(
                UUID.fromString(jwt.getSubject()), request)).build();
    }

    @GetMapping
    public ApiResponse<Page<InvoiceSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID tenancyId,
            @RequestParam(required = false) @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])") String billingMonth,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<Page<InvoiceSummaryResponse>>builder().data(invoiceService.list(
                UUID.fromString(jwt.getSubject()), status, roomId, tenancyId, billingMonth, overdue, PageRequest.of(page, size,
                        Sort.by("createdAt").descending().and(Sort.by("id"))))).build();
    }

    @GetMapping("/{invoiceId}")
    public ApiResponse<InvoiceDetailResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return ApiResponse.<InvoiceDetailResponse>builder().data(invoiceService.get(
                UUID.fromString(jwt.getSubject()), invoiceId)).build();
    }

    @PostMapping("/{invoiceId}/confirm")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<Invoice> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return ApiResponse.<Invoice>builder().data(invoiceService.confirm(
                UUID.fromString(jwt.getSubject()), invoiceId)).build();
    }

    @PostMapping("/{invoiceId}/pay")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Invoice> pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId,
                                    @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.<Invoice>builder().data(invoiceService.pay(
                UUID.fromString(jwt.getSubject()), invoiceId, request)).build();
    }

    @PostMapping("/{invoiceId}/dispute")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<Invoice> dispute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId,
                                        @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.<Invoice>builder().data(invoiceService.dispute(
                UUID.fromString(jwt.getSubject()), invoiceId, request)).build();
    }

    @PostMapping("/{invoiceId}/resolve-dispute")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Invoice> resolveDispute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId,
                                               @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.<Invoice>builder().data(invoiceService.resolveDispute(
                UUID.fromString(jwt.getSubject()), invoiceId, request)).build();
    }
}
