package com.team4.rental.services;

import com.team4.rental.dtos.request.CorrectInvoiceRequest;
import com.team4.rental.dtos.response.InvoiceDetailResponse;
import com.team4.rental.entities.InvoiceCorrection;
import java.util.UUID;
import org.springframework.data.domain.*;

public interface InvoiceCorrectionService {
    InvoiceDetailResponse correct(UUID landlordId, UUID invoiceId, CorrectInvoiceRequest request);
    Page<InvoiceCorrection> history(UUID userId, UUID invoiceId, Pageable pageable);
}
