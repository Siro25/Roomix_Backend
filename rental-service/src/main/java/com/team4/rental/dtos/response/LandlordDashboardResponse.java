package com.team4.rental.dtos.response;

import com.team4.rental.entities.RentalRequest;
import com.team4.rental.entities.Tenancy;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LandlordDashboardResponse {
    long totalHouses;
    long totalRooms;
    long availableRooms;
    long occupiedRooms;
    long reservedRooms;
    long maintenanceRooms;
    double occupancyRate;
    long pendingRequestCount;
    long pendingTenancyCount;
    long activeTenancyCount;
    long overdueInvoiceCount;
    BigDecimal outstandingAmount;
    List<RentalRequest> pendingRequests;
    List<Tenancy> pendingTenancies;
    List<InvoiceSummaryResponse> overdueInvoices;
}
