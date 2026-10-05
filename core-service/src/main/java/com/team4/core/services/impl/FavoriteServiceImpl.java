package com.team4.core.services.impl;

import com.team4.core.dtos.response.FavoriteResponse;
import com.team4.core.dtos.response.RoomSummaryResponse;
import com.team4.core.entities.Favorite;
import com.team4.core.entities.House;
import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.entities.RoomImage;
import com.team4.core.entities.User;
import com.team4.core.enums.PostStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FavoriteRepository;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomImageRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.FavoriteService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RoomImageRepository roomImageRepository;

    @Override
    @Transactional
    public FavoriteResponse add(UUID tenantId, UUID roomId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findFirstByRoomIdOrderByCreatedAtDesc(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_UNAVAILABLE));
        if (post.getStatus() != PostStatus.APPROVED) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        if (favoriteRepository.existsByTenant_IdAndPost_Id(tenantId, post.getId())) {
            throw new AppException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        Favorite favorite;
        try {
            favorite = favoriteRepository.saveAndFlush(Favorite.builder()
                    .tenant(tenant)
                    .post(post)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        Room room = post.getRoom();
        House house = room.getFloor().getHouse();
        String primaryImageUrl = null;
        for (RoomImage image : roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)) {
            if (primaryImageUrl == null || image.isPrimary()) {
                primaryImageUrl = image.getImageUrl();
            }
            if (image.isPrimary()) {
                break;
            }
        }
        BigDecimal rentalPrice = post.getRentalPrice() == null
                ? room.getBasePrice()
                : post.getRentalPrice();

        RoomSummaryResponse roomResponse = RoomSummaryResponse.builder()
                .postId(post.getId())
                .roomId(room.getId())
                .title(post.getTitle())
                .rentalPrice(rentalPrice)
                .area(room.getArea())
                .maxTenants(room.getMaxTenants())
                .addressStreet(house.getAddressStreet())
                .ward(house.getWard())
                .district(house.getDistrict())
                .city(house.getCity())
                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .hasAirConditioner(room.isHasAirConditioner())
                .hasWaterHeater(room.isHasWaterHeater())
                .hasBalcony(room.isHasBalcony())
                .amenitiesDescription(room.getAmenitiesDescription())
                .primaryImageUrl(primaryImageUrl)
                .rented(room.isRented())
                .available(!room.isRented())
                .createdAt(post.getCreatedAt())
                .build();
        return FavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .createdAt(favorite.getCreatedAt())
                .room(roomResponse)
                .build();
    }

    @Override
    @Transactional
    public void remove(UUID tenantId, UUID roomId) {
        List<Favorite> favorites = favoriteRepository.findAllByTenant_IdAndPost_RoomId(
                tenantId, roomId);
        if (favorites.isEmpty()) {
            throw new AppException(ErrorCode.FAVORITE_NOT_FOUND);
        }
        favoriteRepository.deleteAll(favorites);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getAll(UUID tenantId) {
        List<Favorite> favorites = favoriteRepository.findVisibleByTenantId(
                tenantId, PostStatus.APPROVED);
        List<UUID> roomIds = favorites.stream()
                .map(favorite -> favorite.getPost().getRoomId())
                .toList();
        Map<UUID, String> primaryImages = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (RoomImage image : roomImageRepository.findByRoomIdInOrderByDisplayOrderAsc(roomIds)) {
                UUID roomId = image.getRoom().getId();
                if (image.isPrimary() || !primaryImages.containsKey(roomId)) {
                    primaryImages.put(roomId, image.getImageUrl());
                }
            }
        }

        List<FavoriteResponse> responses = new ArrayList<>();
        for (Favorite favorite : favorites) {
            Post post = favorite.getPost();
            Room room = post.getRoom();
            House house = room.getFloor().getHouse();
            BigDecimal rentalPrice = post.getRentalPrice() == null
                    ? room.getBasePrice()
                    : post.getRentalPrice();
            RoomSummaryResponse roomResponse = RoomSummaryResponse.builder()
                    .postId(post.getId())
                    .roomId(room.getId())
                    .title(post.getTitle())
                    .rentalPrice(rentalPrice)
                    .area(room.getArea())
                    .maxTenants(room.getMaxTenants())
                    .addressStreet(house.getAddressStreet())
                    .ward(house.getWard())
                    .district(house.getDistrict())
                    .city(house.getCity())
                    .hasPrivateBathroom(room.isHasPrivateBathroom())
                    .hasAirConditioner(room.isHasAirConditioner())
                    .hasWaterHeater(room.isHasWaterHeater())
                    .hasBalcony(room.isHasBalcony())
                    .amenitiesDescription(room.getAmenitiesDescription())
                    .primaryImageUrl(primaryImages.get(room.getId()))
                    .rented(room.isRented())
                    .available(!room.isRented())
                    .createdAt(post.getCreatedAt())
                    .build();
            responses.add(FavoriteResponse.builder()
                    .favoriteId(favorite.getId())
                    .createdAt(favorite.getCreatedAt())
                    .room(roomResponse)
                    .build());
        }
        return responses;
    }
}
