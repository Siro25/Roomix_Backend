package com.team4.core.services.impl;

import com.team4.core.dtos.request.AuditContext;
import com.team4.core.dtos.response.AdminPostResponse;
import com.team4.core.dtos.response.AdminPostRoomResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.Post;
import com.team4.core.entities.RoomImage;
import com.team4.core.enums.NotificationType;
import com.team4.core.enums.PostStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomImageRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.AdminPostService;
import com.team4.core.services.AuditService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {
    private static final String HOUSE_ENTITY = "HOUSE";
    private static final String APPROVE_POST = "APPROVE_HOUSE_LISTING";
    private static final String REJECT_POST = "REJECT_HOUSE_LISTING";

    private final PostRepository postRepository;
    private final RoomImageRepository roomImageRepository;
    private final AuditService auditService;
    private final AccountNotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPostResponse> getPendingPosts(PostStatus status, Pageable pageable) {
        if (status != PostStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_POST_FILTER);
        }
        Map<UUID, List<Post>> postsByHouse = new LinkedHashMap<>();
        postRepository.findAllByStatusOrderByCreatedAtDesc(status).forEach(post ->
                postsByHouse.computeIfAbsent(
                        post.getRoom().getFloor().getHouse().getId(),
                        ignored -> new ArrayList<>()).add(post));
        List<UUID> roomIds = postsByHouse.values().stream()
                .flatMap(List::stream)
                .map(Post::getRoomId)
                .toList();
        List<RoomImage> roomImages = roomIds.isEmpty()
                ? List.of()
                : roomImageRepository.findByRoomIdInOrderByDisplayOrderAsc(roomIds);
        Map<UUID, List<String>> imagesByRoom = roomImages
                .stream()
                .collect(Collectors.groupingBy(
                        image -> image.getRoom().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(RoomImage::getImageUrl, Collectors.toList())));
        List<AdminPostResponse> houses = postsByHouse.values().stream().map(posts -> {
            AdminPostResponse response = AdminPostResponse.from(posts.getFirst());
            List<AdminPostRoomResponse> rooms = posts.stream()
                    .map(post -> AdminPostRoomResponse.from(
                            post,
                            imagesByRoom.getOrDefault(post.getRoomId(), List.of())))
                    .sorted((first, second) -> {
                        int floorOrder = Integer.compare(first.getFloorNumber(), second.getFloorNumber());
                        return floorOrder != 0
                                ? floorOrder
                                : first.getRoomNumber().compareToIgnoreCase(second.getRoomNumber());
                    })
                    .toList();
            return response.toBuilder()
                    .roomCount(rooms.size())
                    .rooms(rooms)
                    .build();
        }).toList();
        int start = Math.min(pageable.getPageNumber() * pageable.getPageSize(), houses.size());
        int end = Math.min(start + pageable.getPageSize(), houses.size());
        int totalPages = houses.isEmpty()
                ? 0
                : (int) Math.ceil((double) houses.size() / pageable.getPageSize());
        return PageResponse.<AdminPostResponse>builder()
                .content(houses.subList(start, end))
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(houses.size())
                .totalPages(totalPages)
                .first(pageable.getPageNumber() == 0)
                .last(pageable.getPageNumber() >= Math.max(totalPages - 1, 0))
                .build();
    }

    @Override
    @Transactional
    public AdminPostResponse approve(UUID postId, AuditContext context) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new AppException(ErrorCode.POST_NOT_PENDING);
        }

        UUID houseId = post.getRoom().getFloor().getHouse().getId();
        List<Post> housePosts = postRepository
                .findByHouseIdAndLandlordId(houseId, post.getLandlordId())
                .stream()
                .filter(item -> item.getStatus() == PostStatus.PENDING)
                .toList();
        Instant approvedAt = Instant.now();
        housePosts.forEach(item -> item.approve(context.getActorId(), approvedAt));
        postRepository.saveAll(housePosts);

        auditService.record(
                APPROVE_POST,
                HOUSE_ENTITY,
                houseId,
                Map.of("status", PostStatus.PENDING.name()),
                Map.of(
                        "status", PostStatus.APPROVED.name(),
                        "approvedBy", context.getActorId().toString(),
                        "approvedAt", approvedAt.toString()),
                context);
        notificationService.send(
                post.getLandlordId(),
                post.getId(),
                NotificationType.POST_APPROVAL,
                "POST_APPROVED",
                "Tin nhà đã được duyệt",
                "Tin \"" + post.getRoom().getFloor().getHouse().getName()
                        + "\" cùng " + housePosts.size() + " phòng đã được duyệt công khai.",
                "/landlord/listings");
        AdminPostResponse response = AdminPostResponse.from(post);
        return response.toBuilder().roomCount(housePosts.size()).build();
    }

    @Override
    @Transactional
    public AdminPostResponse reject(UUID postId, String reason, AuditContext context) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        if (post.getStatus() != PostStatus.PENDING) {
            throw new AppException(ErrorCode.POST_NOT_PENDING);
        }

        UUID houseId = post.getRoom().getFloor().getHouse().getId();
        List<Post> housePosts = postRepository
                .findByHouseIdAndLandlordId(houseId, post.getLandlordId())
                .stream()
                .filter(item -> item.getStatus() == PostStatus.PENDING)
                .toList();
        housePosts.forEach(item -> item.reject(reason));
        postRepository.saveAll(housePosts);

        auditService.record(
                REJECT_POST,
                HOUSE_ENTITY,
                houseId,
                Map.of("status", PostStatus.PENDING.name()),
                Map.of("status", PostStatus.REJECTED.name(), "rejectReason", reason),
                context);
        notificationService.send(
                post.getLandlordId(),
                post.getId(),
                NotificationType.POST_APPROVAL,
                "POST_REJECTED",
                "Tin nhà chưa được duyệt",
                "Tin \"" + post.getRoom().getFloor().getHouse().getName()
                        + "\" bị từ chối. Lý do: " + reason,
                "/landlord/listings");
        AdminPostResponse response = AdminPostResponse.from(post);
        return response.toBuilder().roomCount(housePosts.size()).build();
    }
}
