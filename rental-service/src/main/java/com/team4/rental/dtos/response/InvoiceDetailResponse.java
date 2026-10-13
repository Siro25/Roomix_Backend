package com.team4.rental.dtos.response;

import com.team4.rental.entities.Invoice;
import com.team4.rental.entities.InvoiceItem;
import com.team4.rental.entities.MeterReading;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailResponse {
    private Invoice invoice;
    private MeterReading meterReading;
    private List<InvoiceItem> items;
}
