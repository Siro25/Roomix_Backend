package com.team4.rental.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.team4.rental.config.VnpayProperties;
import com.team4.rental.entities.Invoice;
import com.team4.rental.entities.Tenancy;
import com.team4.rental.entities.VnpayPaymentAttempt;
import com.team4.rental.enums.InvoiceStatus;
import com.team4.rental.enums.VnpayPaymentStatus;
import com.team4.rental.repositories.InvoiceRepository;
import com.team4.rental.repositories.TenancyRepository;
import com.team4.rental.repositories.VnpayPaymentAttemptRepository;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VnpayServiceTest {
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final TenancyRepository tenancies = mock(TenancyRepository.class);
    private final VnpayPaymentAttemptRepository attempts = mock(VnpayPaymentAttemptRepository.class);
    private final VnpayProperties config = new VnpayProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC);
    private final VnpayService service = new VnpayService(config, invoices, tenancies, attempts, clock);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void configure() {
        config.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setTmnCode("TESTCODE");
        config.setHashSecret("test-secret-for-signature");
        config.setReturnUrl("https://example.com/api/invoices/vnpay/return");
    }

    @Test
    void createsSignedUrlFromInvoiceAmount() throws Exception {
        var invoice = Invoice.builder().id(invoiceId).invoiceCode("INV-123")
                .tenancyId(UUID.randomUUID()).status(InvoiceStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150000.00")).paidAmount(BigDecimal.ZERO).build();
        when(invoices.lockById(invoiceId)).thenReturn(Optional.of(invoice));
        when(tenancies.findVisible(invoice.getTenancyId(), tenantId)).thenReturn(Optional.of(
                Tenancy.builder().tenantId(tenantId).build()));

        var result = service.createPayment(tenantId, invoiceId, "127.0.0.1");
        var query = result.getPaymentUrl().split("\\?", 2)[1];
        var fields = Arrays.stream(query.split("&")).map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> part[0], part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
        assertEquals("15000000", fields.get("vnp_Amount"));
        assertEquals("20261025100000", fields.get("vnp_CreateDate"));
        assertEquals("20261025101500", fields.get("vnp_ExpireDate"));
        assertEquals(result.getTxnRef(), fields.get("vnp_TxnRef"));
        assertEquals(128, fields.get("vnp_SecureHash").length());
        String unsignedQuery = query.substring(0, query.lastIndexOf("&vnp_SecureHash="));
        var mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(config.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        assertEquals(java.util.HexFormat.of().formatHex(mac.doFinal(
                unsignedQuery.getBytes(StandardCharsets.UTF_8))), fields.get("vnp_SecureHash"));
        verify(attempts).save(any(VnpayPaymentAttempt.class));
    }

    @Test
    void acceptsValidIpnOnceAndMarksInvoicePaid() throws Exception {
        var invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150000.00")).paidAmount(BigDecimal.ZERO).build();
        var attempt = VnpayPaymentAttempt.builder().invoiceId(invoiceId).txnRef("test-ref")
                .amount(invoice.getTotalAmount()).status(VnpayPaymentStatus.PENDING).build();
        when(attempts.findByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(attempts.lockByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(invoices.lockById(invoiceId)).thenReturn(Optional.of(invoice));
        var fields = signedIpn("15000000");

        assertEquals("00", service.processIpn(fields).get("RspCode"));
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals("VNPAY", invoice.getPaymentMethod());
        assertEquals(VnpayPaymentStatus.SUCCESS, attempt.getStatus());
        assertEquals("02", service.processIpn(fields).get("RspCode"));
        verify(invoices, times(1)).save(invoice);
    }

    @Test
    void rejectsInvalidSignatureAndWrongAmountWithoutChangingInvoice() throws Exception {
        var invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150000.00")).paidAmount(BigDecimal.ZERO).build();
        var attempt = VnpayPaymentAttempt.builder().invoiceId(invoiceId).txnRef("test-ref")
                .amount(invoice.getTotalAmount()).status(VnpayPaymentStatus.PENDING).build();
        when(attempts.findByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(attempts.lockByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(invoices.lockById(invoiceId)).thenReturn(Optional.of(invoice));
        var fields = signedIpn("10000000");

        assertEquals("04", service.processIpn(fields).get("RspCode"));
        fields.put("vnp_Amount", "15000000");
        assertEquals("97", service.processIpn(fields).get("RspCode"));
        assertEquals(InvoiceStatus.CONFIRMED, invoice.getStatus());
        verify(invoices, never()).save(any());
    }

    @Test
    void returnUrlNeverMarksInvoicePaid() throws Exception {
        var attempt = VnpayPaymentAttempt.builder().invoiceId(invoiceId).txnRef("test-ref")
                .status(VnpayPaymentStatus.PENDING).build();
        when(attempts.findByTxnRef("test-ref")).thenReturn(Optional.of(attempt));

        var result = service.returnResult(signedIpn("15000000"));

        assertEquals("PENDING", result.getPaymentStatus());
        verify(invoices, never()).save(any());
        verify(attempts, never()).save(any());
    }

    @Test
    void localConfirmationMarksPendingAttemptAndInvoicePaid() {
        var invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150000.00")).paidAmount(BigDecimal.ZERO).build();
        var attempt = VnpayPaymentAttempt.builder().invoiceId(invoiceId).txnRef("test-ref")
                .amount(invoice.getTotalAmount()).status(VnpayPaymentStatus.PENDING)
                .expiresAt(Instant.parse("2026-10-25T04:00:00Z")).build();
        when(attempts.findByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(attempts.findByInvoiceIdAndStatus(invoiceId, VnpayPaymentStatus.PENDING)).thenReturn(Optional.of(attempt));
        when(attempts.lockByTxnRef("test-ref")).thenReturn(Optional.of(attempt));
        when(invoices.lockById(invoiceId)).thenReturn(Optional.of(invoice));
        when(tenancies.findVisible(invoice.getTenancyId(), tenantId)).thenReturn(Optional.of(
                Tenancy.builder().tenantId(tenantId).build()));

        var result = service.confirmLocally(tenantId, invoiceId);

        assertEquals("SUCCESS", result.getPaymentStatus());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals("VNPAY_LOCAL_SIMULATION", invoice.getPaymentMethod());
        verify(invoices).save(invoice);
    }

    private Map<String, String> signedIpn(String amount) throws Exception {
        var fields = new TreeMap<String, String>();
        fields.put("vnp_Amount", amount);
        fields.put("vnp_ResponseCode", "00");
        fields.put("vnp_TmnCode", "TESTCODE");
        fields.put("vnp_TransactionNo", "123456789");
        fields.put("vnp_TransactionStatus", "00");
        fields.put("vnp_TxnRef", "test-ref");
        String payload = fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        var mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(config.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        fields.put("vnp_SecureHash", java.util.HexFormat.of().formatHex(
                mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
        return fields;
    }
}
