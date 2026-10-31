package com.team4.core.services;

import com.team4.core.dtos.response.AdminDashboardResponse;
import java.util.UUID;

public interface AdminDashboardService {
    AdminDashboardResponse getDashboard(UUID adminId);
}
