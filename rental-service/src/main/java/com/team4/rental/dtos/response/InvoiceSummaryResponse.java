package com.team4.rental.dtos.response;

import com.team4.rental.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryResponse {
    private UUID id;
    private String invoiceCode;
    private UUID tenancyId;
    private String billingMonth;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private boolean overdue;
    private boolean disputed;
}
