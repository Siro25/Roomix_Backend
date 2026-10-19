package com.team4.rental.controllers;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.entities.InvoiceReminder;
import com.team4.rental.services.InvoiceReminderService;
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
public class InvoiceReminderController {
    private final InvoiceReminderService reminderService;

    @GetMapping("/reminders")
    public ApiResponse<Page<InvoiceReminder>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.<Page<InvoiceReminder>>builder().data(reminderService.list(
                UUID.fromString(jwt.getSubject()), PageRequest.of(page, size, Sort.by("createdAt").descending()
                        .and(Sort.by("id"))))).build();
    }

    @PostMapping("/{invoiceId}/remind")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<InvoiceReminder> remind(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return ApiResponse.<InvoiceReminder>builder().data(reminderService.remind(
                UUID.fromString(jwt.getSubject()), invoiceId)).build();
    }
}
