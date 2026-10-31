package com.team4.rental.services.impl;

import com.team4.rental.dtos.response.CoreLandlordDashboardResponse;
import com.team4.rental.dtos.response.InvoiceSummaryResponse;
import com.team4.rental.dtos.response.LandlordDashboardResponse;
import com.team4.rental.enums.RequestStatus;
import com.team4.rental.enums.TenancyStatus;
import com.team4.rental.external.CoreServiceClient;
import com.team4.rental.repositories.InvoiceRepository;
import com.team4.rental.repositories.RentalRequestRepository;
import com.team4.rental.repositories.TenancyRepository;
import com.team4.rental.services.LandlordDashboardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandlordDashboardServiceImpl implements LandlordDashboardService {
    private static final int PREVIEW_SIZE = 5;

    private final RentalRequestRepository rentalRequestRepository;
    private final TenancyRepository tenancyRepository;
    private final InvoiceRepository invoiceRepository;
    private final CoreServiceClient coreServiceClient;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public LandlordDashboardResponse getDashboard(UUID landlordId, String accessToken) {
        CoreLandlordDashboardResponse properties = coreServiceClient.getLandlordDashboard(
                landlordId,
                accessToken);
        var newest = PageRequest.of(
                0,
                PREVIEW_SIZE,
                Sort.by("createdAt").descending().and(Sort.by("id")));
        LocalDate today = LocalDate.now(clock);

        return LandlordDashboardResponse.builder()
                .totalHouses(properties.getTotalHouses())
                .totalRooms(properties.getTotalRooms())
                .availableRooms(properties.getAvailableRooms())
                .occupiedRooms(properties.getOccupiedRooms())
                .reservedRooms(properties.getReservedRooms())
                .maintenanceRooms(properties.getMaintenanceRooms())
                .occupancyRate(calculateOccupancyRate(
                        properties.getOccupiedRooms(),
                        properties.getTotalRooms()))
                .pendingRequestCount(rentalRequestRepository.countByLandlordIdAndStatus(
                        landlordId,
                        RequestStatus.PENDING))
                .pendingTenancyCount(tenancyRepository.countByLandlordIdAndStatus(
                        landlordId,
                        TenancyStatus.PENDING))
                .activeTenancyCount(tenancyRepository.countByLandlordIdAndStatus(
                        landlordId,
                        TenancyStatus.ACTIVE))
                .overdueInvoiceCount(invoiceRepository.countOverdueByLandlord(landlordId, today))
                .outstandingAmount(invoiceRepository.sumOutstandingByLandlord(landlordId))
                .pendingRequests(rentalRequestRepository.findByLandlordIdAndStatus(
                                landlordId,
                                RequestStatus.PENDING,
                                newest)
                        .getContent())
                .pendingTenancies(tenancyRepository.findByLandlordIdAndStatus(
                                landlordId,
                                TenancyStatus.PENDING,
                                newest)
                        .getContent())
                .overdueInvoices(invoiceRepository.findOverdueByLandlord(
                                landlordId,
                                today,
                                newest)
                        .map(invoice -> InvoiceSummaryResponse.builder()
                                .id(invoice.getId())
                                .invoiceCode(invoice.getInvoiceCode())
                                .tenancyId(invoice.getTenancyId())
                                .billingMonth(invoice.getBillingMonth())
                                .dueDate(invoice.getDueDate())
                                .status(invoice.getStatus())
                                .totalAmount(invoice.getTotalAmount())
                                .paidAmount(invoice.getPaidAmount())
                                .outstandingAmount(invoice.getTotalAmount()
                                        .subtract(invoice.getPaidAmount()))
                                .overdue(true)
                                .disputed(invoice.isDisputed())
                                .build())
                        .getContent())
                .build();
    }

    private double calculateOccupancyRate(long occupiedRooms, long totalRooms) {
        if (totalRooms == 0) {
            return 0;
        }
        return BigDecimal.valueOf(occupiedRooms)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
