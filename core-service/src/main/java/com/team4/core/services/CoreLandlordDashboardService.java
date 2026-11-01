package com.team4.core.services;

import com.team4.core.dtos.response.CoreLandlordDashboardResponse;
import java.util.UUID;

public interface CoreLandlordDashboardService {
    CoreLandlordDashboardResponse getPropertySummary(UUID landlordId);
}
