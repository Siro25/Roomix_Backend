package com.team4.rental.services;

import com.team4.rental.config.VnpayProperties;
import com.team4.rental.dtos.response.VnpayPaymentResponse;
import com.team4.rental.dtos.response.VnpayReturnResponse;
import com.team4.rental.entities.VnpayPaymentAttempt;
import com.team4.rental.enums.InvoiceStatus;
import com.team4.rental.enums.VnpayPaymentStatus;
import com.team4.rental.exceptions.AppException;
import com.team4.rental.exceptions.ErrorCode;
import com.team4.rental.repositories.InvoiceRepository;
import com.team4.rental.repositories.TenancyRepository;
import com.team4.rental.repositories.VnpayPaymentAttemptRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(VIETNAM);

    private final VnpayProperties config;
    private final InvoiceRepository invoices;
    private final TenancyRepository tenancies;
    private final VnpayPaymentAttemptRepository attempts;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public VnpayPaymentResponse createPayment(UUID tenantId, UUID invoiceId, String ipAddress) {
        if (config.getTmnCode() == null || config.getTmnCode().isBlank()
                || config.getHashSecret() == null || config.getHashSecret().isBlank()
                || config.getReturnUrl() == null || config.getReturnUrl().isBlank()) {
            throw new AppException(ErrorCode.VNPAY_NOT_CONFIGURED);
        }
        var invoice = invoices.lockById(invoiceId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var tenancy = tenancies.findVisible(invoice.getTenancyId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!tenantId.equals(tenancy.getTenantId())) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        if (invoice.getStatus() != InvoiceStatus.CONFIRMED || invoice.isDisputed()
                || invoice.getPaidAmount().signum() != 0) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        BigDecimal amount = invoice.getTotalAmount();
        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0
                || amount.compareTo(new BigDecimal("9999999999")) > 0) {
            throw new AppException(ErrorCode.VNPAY_AMOUNT_INVALID);
        }

        var now = clock.instant();
        var pending = attempts.findByInvoiceIdAndStatus(invoiceId, VnpayPaymentStatus.PENDING);
        if (pending.isPresent()) {
            var existing = pending.get();
            String encodedReturnUrl = URLEncoder.encode(config.getReturnUrl(), StandardCharsets.UTF_8);
            boolean usesCurrentReturnUrl = existing.getPaymentUrl() != null
                    && existing.getPaymentUrl().contains("vnp_ReturnUrl=" + encodedReturnUrl);
            if (existing.getExpiresAt().isAfter(now) && usesCurrentReturnUrl) {
                return VnpayPaymentResponse.builder().txnRef(existing.getTxnRef())
                        .paymentUrl(existing.getPaymentUrl()).expiresAt(existing.getExpiresAt()).build();
            }
            existing.setStatus(VnpayPaymentStatus.EXPIRED);
            attempts.saveAndFlush(existing);
        }

        var expiresAt = now.plus(Duration.ofMinutes(15));
        var txnRef = UUID.randomUUID().toString().replace("-", "");
        var parameters = new TreeMap<String, String>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", config.getTmnCode());
        parameters.put("vnp_Amount", amount.movePointRight(2).toBigIntegerExact().toString());
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", txnRef);
        parameters.put("vnp_OrderInfo", "Thanh toan hoa don " + invoice.getInvoiceCode());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", config.getReturnUrl());
        parameters.put("vnp_IpAddr", ipAddress);
        parameters.put("vnp_CreateDate", VNPAY_DATE.format(now));
        parameters.put("vnp_ExpireDate", VNPAY_DATE.format(expiresAt));

        String query = encode(parameters);
        String paymentUrl = config.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + sign(query);
        VnpayPaymentAttempt attempt = VnpayPaymentAttempt.builder()
                .id(UUID.randomUUID()).invoiceId(invoiceId)
                .txnRef(txnRef).amount(amount).status(VnpayPaymentStatus.PENDING)
                .paymentUrl(paymentUrl).createdAt(now).expiresAt(expiresAt).build();
        attempts.save(attempt);
        auditService.record(
                tenantId,
                "CREATE_VNPAY_PAYMENT",
                "VNPAY_PAYMENT",
                attempt.getId(),
                Map.of(),
                Map.of(
                        "invoiceId", invoiceId.toString(),
                        "txnRef", txnRef,
                        "status", VnpayPaymentStatus.PENDING.name(),
                        "amount", amount.toPlainString(),
                        "expiresAt", expiresAt.toString()));
        return VnpayPaymentResponse.builder().txnRef(txnRef).paymentUrl(paymentUrl)
                .expiresAt(expiresAt).build();
    }

    @Transactional
    public Map<String, String> processIpn(Map<String, String> parameters) {
        if (!validSignature(parameters) || config.getTmnCode() == null
                || !config.getTmnCode().equals(parameters.get("vnp_TmnCode"))) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }
        var found = attempts.findByTxnRef(parameters.get("vnp_TxnRef"));
        if (found.isEmpty()) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }
        var invoice = invoices.lockById(found.get().getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var attempt = attempts.lockByTxnRef(parameters.get("vnp_TxnRef"))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        String expectedAmount = attempt.getAmount().movePointRight(2).toBigIntegerExact().toString();
        if (!expectedAmount.equals(parameters.get("vnp_Amount"))
                || invoice.getTotalAmount().compareTo(attempt.getAmount()) != 0) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }
        if (attempt.getStatus() == VnpayPaymentStatus.EXPIRED
                && "00".equals(parameters.get("vnp_ResponseCode"))
                && "00".equals(parameters.get("vnp_TransactionStatus"))) {
            log.error("VNPAY payment needs reconciliation: expired txnRef={}, invoiceId={}",
                    attempt.getTxnRef(), attempt.getInvoiceId());
            return Map.of("RspCode", "99", "Message", "Expired order requires reconciliation");
        }
        if (attempt.getStatus() != VnpayPaymentStatus.PENDING || invoice.getStatus() == InvoiceStatus.PAID) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }
        if (parameters.get("vnp_ResponseCode") == null
                || parameters.get("vnp_TransactionStatus") == null) {
            return Map.of("RspCode", "99", "Message", "Missing transaction result");
        }
        boolean success = "00".equals(parameters.get("vnp_ResponseCode"))
                && "00".equals(parameters.get("vnp_TransactionStatus"));
        if (success && (invoice.getStatus() != InvoiceStatus.CONFIRMED || invoice.isDisputed()
                || invoice.getPaidAmount().signum() != 0
                || parameters.get("vnp_TransactionNo") == null)) {
            log.error("VNPAY payment needs reconciliation: invoice changed, txnRef={}, invoiceId={}",
                    attempt.getTxnRef(), attempt.getInvoiceId());
            return Map.of("RspCode", "04", "Message", "Invoice changed");
        }
        attempt.setStatus(success ? VnpayPaymentStatus.SUCCESS : VnpayPaymentStatus.FAILED);
        attempt.setResponseCode(parameters.get("vnp_ResponseCode"));
        attempt.setTransactionNo(parameters.get("vnp_TransactionNo"));
        attempt.setCompletedAt(clock.instant());
        attempts.save(attempt);
        if (success) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAmount(invoice.getTotalAmount());
            invoice.setPaymentMethod("VNPAY");
            invoice.setPaymentDate(clock.instant());
            invoices.save(invoice);
        }
        auditService.record(
                null,
                "COMPLETE_VNPAY_PAYMENT",
                "VNPAY_PAYMENT",
                attempt.getId(),
                Map.of("status", VnpayPaymentStatus.PENDING.name()),
                Map.of(
                        "status", attempt.getStatus().name(),
                        "invoiceId", invoice.getId().toString(),
                        "responseCode", attempt.getResponseCode(),
                        "invoiceStatus", invoice.getStatus().name()));
        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    @Transactional
    public VnpayReturnResponse returnResult(Map<String, String> parameters) {
        if (!validSignature(parameters) || config.getTmnCode() == null
                || !config.getTmnCode().equals(parameters.get("vnp_TmnCode"))) {
            return VnpayReturnResponse.builder().paymentStatus("INVALID")
                    .message("Chữ ký VNPAY không hợp lệ").build();
        }
        var attempt = attempts.findByTxnRef(parameters.get("vnp_TxnRef"));
        if (attempt.isEmpty()) {
            return VnpayReturnResponse.builder().paymentStatus("UNKNOWN")
                    .message("Không tìm thấy giao dịch").build();
        }
        var payment = attempt.get();
        if (config.isConfirmOnReturn() && payment.getStatus() == VnpayPaymentStatus.PENDING) {
            processIpn(parameters);
            payment = attempts.findByTxnRef(parameters.get("vnp_TxnRef"))
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        }
        return VnpayReturnResponse.builder().invoiceId(payment.getInvoiceId())
                .txnRef(payment.getTxnRef()).responseCode(parameters.get("vnp_ResponseCode"))
                .paymentStatus(payment.getStatus().name())
                .message(payment.getStatus() == VnpayPaymentStatus.SUCCESS
                        ? "Đã ghi nhận thanh toán" : "Đang chờ kết quả IPN hoặc giao dịch không thành công")
                .build();
    }

    @Transactional
    public VnpayReturnResponse confirmLocally(UUID tenantId, UUID invoiceId) {
        var invoice = invoices.lockById(invoiceId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        var tenancy = tenancies.findVisible(invoice.getTenancyId(), tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!tenantId.equals(tenancy.getTenantId())) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        var pending = attempts.findByInvoiceIdAndStatus(invoiceId, VnpayPaymentStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE));
        var attempt = attempts.lockByTxnRef(pending.getTxnRef())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE));
        if (!attempt.getExpiresAt().isAfter(clock.instant()) || invoice.getStatus() != InvoiceStatus.CONFIRMED
                || invoice.isDisputed() || invoice.getPaidAmount().signum() != 0) {
            throw new AppException(ErrorCode.INVALID_STATE);
        }
        attempt.setStatus(VnpayPaymentStatus.SUCCESS);
        attempt.setResponseCode("00");
        attempt.setTransactionNo("LOCAL-" + attempt.getTxnRef());
        attempt.setCompletedAt(clock.instant());
        attempts.save(attempt);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(invoice.getTotalAmount());
        invoice.setPaymentMethod("VNPAY_LOCAL_SIMULATION");
        invoice.setPaymentDate(clock.instant());
        invoices.save(invoice);
        auditService.record(
                tenantId,
                "SIMULATE_VNPAY_PAYMENT",
                "VNPAY_PAYMENT",
                attempt.getId(),
                Map.of("status", VnpayPaymentStatus.PENDING.name()),
                Map.of(
                        "status", VnpayPaymentStatus.SUCCESS.name(),
                        "invoiceId", invoiceId.toString(),
                        "invoiceStatus", InvoiceStatus.PAID.name(),
                        "paymentMethod", invoice.getPaymentMethod()));
        return VnpayReturnResponse.builder().invoiceId(invoiceId).txnRef(attempt.getTxnRef())
                .responseCode("00").paymentStatus(VnpayPaymentStatus.SUCCESS.name())
                .message("Đã mô phỏng thanh toán VNPAY trên môi trường local").build();
    }

    private boolean validSignature(Map<String, String> parameters) {
        String received = parameters.get("vnp_SecureHash");
        if (received == null || !received.matches("(?i)[0-9a-f]{128}")
                || config.getHashSecret() == null || config.getHashSecret().isBlank()) {
            return false;
        }
        var signedFields = new TreeMap<String, String>();
        parameters.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType") && value != null && !value.isBlank()) {
                signedFields.put(key, value);
            }
        });
        String expected = sign(encode(signedFields));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private String encode(Map<String, String> parameters) {
        var query = new StringBuilder();
        parameters.forEach((key, value) -> {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(URLEncoder.encode(key, StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return query.toString();
    }

    private String sign(String query) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(config.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(query.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign VNPAY request", exception);
        }
    }
}
