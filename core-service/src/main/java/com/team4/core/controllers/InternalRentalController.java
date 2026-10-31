package com.team4.core.controllers;

import com.team4.core.dtos.request.ContactCardNotificationRequest;
import com.team4.core.dtos.request.RentalNotificationRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.CoreAccountResponse;
import com.team4.core.dtos.response.CoreLandlordDashboardResponse;
import com.team4.core.dtos.response.CoreRoomResponse;
import com.team4.core.dtos.response.LandlordContactResponse;
import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.entities.User;
import com.team4.core.enums.NotificationType;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.CoreLandlordDashboardService;
import com.team4.core.services.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/rental")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT', 'LANDLORD')")
public class InternalRentalController {
    private static final Map<String, Set<String>> ALLOWED_NOTIFICATION_EVENTS = Map.of(
            "TENANT", Set.of(
                    "RENTAL_REQUEST_CREATED",
                    "RENTAL_REQUEST_CANCELLED",
                    "TENANCY_CANCELLED",
                    "INVOICE_CONFIRMED",
                    "INVOICE_DISPUTED"),
            "LANDLORD", Set.of(
                    "RENTAL_REQUEST_REJECTED",
                    "TENANCY_ACTIVATED",
                    "TENANCY_CANCELLED",
                    "TENANCY_ENDED",
                    "INVOICE_CREATED",
                    "INVOICE_PAID",
                    "INVOICE_DISPUTE_RESOLVED"));

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AccountNotificationService notificationService;
    private final CoreLandlordDashboardService landlordDashboardService;

    @GetMapping("/rooms/{roomId}")
    @Transactional(readOnly = true)
    public ApiResponse<CoreRoomResponse> room(@PathVariable UUID roomId) {
        Post post = postRepository.findFirstByRoomIdOrderByCreatedAtDesc(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Room room = post.getRoom();

        CoreRoomResponse response = CoreRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .landlordId(room.getFloor().getHouse().getLandlordId())
                .postStatus(post.getStatus().name())
                .roomStatus(room.getStatus().name())
                .rented(room.isRented())
                .availableFrom(post.getAvailableFrom())
                .build();

        return ApiResponse.<CoreRoomResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/account")
    public ApiResponse<CoreAccountResponse> account(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepository.findById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        CoreAccountResponse response = CoreAccountResponse.builder()
                .id(user.getId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();

        return ApiResponse.<CoreAccountResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/landlords/{landlordId}/contact")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<LandlordContactResponse> landlordContact(
            @PathVariable UUID landlordId,
            @AuthenticationPrincipal Jwt jwt) {
        if (!landlordId.equals(UUID.fromString(jwt.getSubject()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return ApiResponse.<LandlordContactResponse>builder()
                .data(userService.getLandlordContact(landlordId))
                .build();
    }

    @GetMapping("/landlords/{landlordId}/dashboard")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<CoreLandlordDashboardResponse> landlordDashboard(
            @PathVariable UUID landlordId,
            @AuthenticationPrincipal Jwt jwt) {
        if (!landlordId.equals(UUID.fromString(jwt.getSubject()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return ApiResponse.<CoreLandlordDashboardResponse>builder()
                .data(landlordDashboardService.getPropertySummary(landlordId))
                .build();
    }

    @PostMapping("/notifications/contact-card")
    @PreAuthorize("hasRole('LANDLORD')")
    public ApiResponse<Void> sendContactCardNotification(
            @Valid @RequestBody ContactCardNotificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID landlordId = UUID.fromString(jwt.getSubject());
        LandlordContactResponse landlord = userService.getLandlordContact(landlordId);

        if (!userRepository.existsById(request.getTenantId())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        String zalo = landlord.getZalo() == null ? "Chưa cập nhật" : landlord.getZalo();
        String content = "Chủ trọ: " + landlord.getFullName()
                + " | Số điện thoại: " + landlord.getPhoneNumber()
                + " | Zalo: " + zalo;

        notificationService.send(
                request.getTenantId(),
                request.getRentalRequestId(),
                NotificationType.CONTACT_CARD,
                "CONTACT_CARD_ISSUED",
                "Chủ trọ đã chấp nhận yêu cầu thuê",
                content,
                "/requests");

        return ApiResponse.<Void>builder()
                .message("Đã gửi danh thiếp chủ trọ")
                .build();
    }

    @PostMapping("/notifications")
    public ApiResponse<Void> sendRentalNotification(
            @Valid @RequestBody RentalNotificationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (!ALLOWED_NOTIFICATION_EVENTS.getOrDefault(role, Set.of())
                .contains(request.getEvent())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (!userRepository.existsById(request.getUserId())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        notificationService.send(
                request.getUserId(),
                request.getReferenceId(),
                request.getType(),
                request.getEvent(),
                request.getTitle(),
                request.getContent(),
                request.getTargetUrl());

        return ApiResponse.<Void>builder()
                .message("Đã gửi thông báo nghiệp vụ thuê phòng")
                .build();
    }
}
