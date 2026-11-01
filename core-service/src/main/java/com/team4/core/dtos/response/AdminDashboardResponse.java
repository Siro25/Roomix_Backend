package com.team4.core.dtos.response;

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
public class AdminDashboardResponse {
    long totalUsers;
    long totalTenants;
    long totalLandlords;
    long activeUsers;
    long lockedUsers;
    long pendingLandlords;
    long totalRooms;
    long availableRooms;
    long rentedRooms;
    long approvedPosts;
    long pendingPostCount;
    long openReportCount;
    long unreadNotificationCount;
    List<AdminPostResponse> pendingPosts;
    List<AdminUserResponse> pendingLandlordProfiles;
    List<ViolationReportResponse> openReports;
}
