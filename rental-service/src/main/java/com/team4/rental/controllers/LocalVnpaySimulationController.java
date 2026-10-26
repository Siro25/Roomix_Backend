package com.team4.rental.controllers;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.VnpayReturnResponse;
import com.team4.rental.services.VnpayService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Profile("local")
public class LocalVnpaySimulationController {
    private final VnpayService vnpayService;

    @PostMapping("/{invoiceId}/vnpay/dev-confirm")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<VnpayReturnResponse> confirmPayment(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invoiceId) {
        return ApiResponse.<VnpayReturnResponse>builder()
                .data(vnpayService.confirmLocally(UUID.fromString(jwt.getSubject()), invoiceId)).build();
    }
}
