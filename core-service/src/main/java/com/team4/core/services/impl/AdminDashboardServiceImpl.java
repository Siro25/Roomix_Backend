package com.team4.core.services.impl;

import com.team4.core.dtos.response.AdminDashboardResponse;
import com.team4.core.dtos.response.AdminPostResponse;
import com.team4.core.dtos.response.AdminUserResponse;
import com.team4.core.dtos.response.ViolationReportResponse;
import com.team4.core.enums.PostStatus;
import com.team4.core.enums.ReportStatus;
import com.team4.core.enums.Role;
import com.team4.core.enums.RoomStatus;
import com.team4.core.enums.UserStatus;
import com.team4.core.repositories.NotificationRepository;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.repositories.ViolationReportRepository;
import com.team4.core.services.AdminDashboardService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final int PREVIEW_SIZE = 5;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PostRepository postRepository;
    private final ViolationReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(UUID adminId) {
        var newest = PageRequest.of(0, PREVIEW_SIZE, Sort.by("createdAt").descending());
        List<ReportStatus> openStatuses = List.of(ReportStatus.PENDING, ReportStatus.REVIEWING);

        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalTenants(userRepository.countByRole(Role.TENANT))
                .totalLandlords(userRepository.countByRole(Role.LANDLORD))
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .lockedUsers(userRepository.countByStatus(UserStatus.LOCKED))
                .pendingLandlords(userRepository.countByRoleAndStatus(
                        Role.LANDLORD,
                        UserStatus.PENDING_VERIFICATION))
                .totalRooms(roomRepository.count())
                .availableRooms(roomRepository.countByStatusAndRentedFalse(RoomStatus.AVAILABLE))
                .rentedRooms(roomRepository.countByRentedTrue())
                .approvedPosts(postRepository.countByStatus(PostStatus.APPROVED))
                .pendingPostCount(postRepository.countByStatus(PostStatus.PENDING))
                .openReportCount(reportRepository.countByStatusIn(openStatuses))
                .unreadNotificationCount(notificationRepository.countByUserIdAndReadFalse(adminId))
                .pendingPosts(postRepository.findByStatus(PostStatus.PENDING, newest).stream()
                        .map(AdminPostResponse::from)
                        .toList())
                .pendingLandlordProfiles(userRepository.findByRoleAndStatus(
                                Role.LANDLORD,
                                UserStatus.PENDING_VERIFICATION,
                                newest)
                        .stream()
                        .map(AdminUserResponse::from)
                        .toList())
                .openReports(reportRepository.findByStatusIn(openStatuses, newest).stream()
                        .map(ViolationReportResponse::from)
                        .toList())
                .build();
    }
}
