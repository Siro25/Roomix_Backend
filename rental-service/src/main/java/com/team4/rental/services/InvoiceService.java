package com.team4.rental.services;

import com.team4.rental.dtos.request.*;
import com.team4.rental.dtos.response.InvoiceDetailResponse;
import com.team4.rental.entities.Invoice;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface InvoiceService {
    InvoiceDetailResponse create(UUID landlordId, CreateInvoiceRequest request);
    Page<Invoice> list(UUID userId, Pageable pageable);
    InvoiceDetailResponse get(UUID userId, UUID invoiceId);
    Invoice confirm(UUID tenantId, UUID invoiceId);
    Invoice pay(UUID landlordId, UUID invoiceId, PaymentRequest request);
    Invoice dispute(UUID tenantId, UUID invoiceId, ReasonRequest request);
    Invoice resolveDispute(UUID landlordId, UUID invoiceId, ReasonRequest request);
}
