package com.team4.rental.services;

import com.team4.rental.dtos.response.LandlordDashboardResponse;
import java.util.UUID;

public interface LandlordDashboardService {
    LandlordDashboardResponse getDashboard(UUID landlordId, String accessToken);
}
