package com.team4.rental.controllers;

import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.VnpayPaymentResponse;
import com.team4.rental.dtos.response.VnpayReturnResponse;
import com.team4.rental.services.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class VnpayController {
    private final VnpayService vnpayService;

    @PostMapping("/{invoiceId}/vnpay/payment-url")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<VnpayPaymentResponse> createPayment(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invoiceId, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ApiResponse.<VnpayPaymentResponse>builder().data(vnpayService.createPayment(
                UUID.fromString(jwt.getSubject()), invoiceId, ipAddress)).build();
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> parameters) {
        try {
            return vnpayService.processIpn(parameters);
        } catch (RuntimeException exception) {
            log.error("Failed to process VNPAY IPN", exception);
            return Map.of("RspCode", "99", "Message", "Internal error");
        }
    }

    @GetMapping("/vnpay/return")
    public ApiResponse<VnpayReturnResponse> returnResult(@RequestParam Map<String, String> parameters) {
        return ApiResponse.<VnpayReturnResponse>builder()
                .data(vnpayService.returnResult(parameters)).build();
    }
}
