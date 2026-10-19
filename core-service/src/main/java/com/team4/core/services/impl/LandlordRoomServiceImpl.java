package com.team4.core.services.impl;

import com.team4.core.dtos.request.LandlordRoomRequest;
import com.team4.core.dtos.response.LandlordRoomResponse;
import com.team4.core.entities.Floor;
import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.entities.RoomImage;
import com.team4.core.enums.PostStatus;
import com.team4.core.enums.RoomStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FloorRepository;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomImageRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.services.ImageStorageService;
import com.team4.core.services.LandlordRoomService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LandlordRoomServiceImpl implements LandlordRoomService {
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final PostRepository postRepository;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public LandlordRoomResponse create(
            UUID landlordId,
            LandlordRoomRequest request,
            List<MultipartFile> images) {
        Floor floor = floorRepository
                .findByIdAndHouseIdAndHouseLandlordId(
                        request.getFloorId(), request.getHouseId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));

        Room room = roomRepository.save(Room.builder()
                .floor(floor)
                .roomNumber(request.getRoomNumber().strip())
                .area(request.getArea())
                .basePrice(request.getBasePrice())
                .maxTenants(request.getMaxTenants())
                .status(RoomStatus.AVAILABLE)
                .hasPrivateBathroom(request.isHasPrivateBathroom())
                .hasAirConditioner(request.isHasAirConditioner())
                .hasWaterHeater(request.isHasWaterHeater())
                .hasBalcony(request.isHasBalcony())
                .amenitiesDescription(request.getAmenitiesDescription())
                .rented(false)
                .build());

        Post post = postRepository.save(Post.builder()
                .landlordId(landlordId)
                .roomId(room.getId())
                .title(request.getTitle().strip())
                .description(request.getDescription().strip())
                .rentalPrice(request.getBasePrice())
                .depositAmount(request.getDepositAmount())
                .availableFrom(request.getAvailableFrom())
                .status(PostStatus.DRAFT)
                .build());

        List<String> imageUrls = new ArrayList<>();
        List<MultipartFile> uploadedImages = images == null ? List.of() : images;
        for (int index = 0; index < uploadedImages.size(); index++) {
            String imageUrl = imageStorageService.upload(uploadedImages.get(index));
            imageUrls.add(imageUrl);
            roomImageRepository.save(RoomImage.builder()
                    .room(room)
                    .imageUrl(imageUrl)
                    .primary(index == 0)
                    .displayOrder(index)
                    .build());
        }

        return LandlordRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .houseId(floor.getHouse().getId())
                .floorId(floor.getId())
                .roomNumber(room.getRoomNumber())
                .area(room.getArea())
                .basePrice(room.getBasePrice())
                .roomStatus(room.getStatus())
                .postStatus(post.getStatus())
                .rented(room.isRented())
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional
    public LandlordRoomResponse update(
            UUID landlordId,
            UUID roomId,
            LandlordRoomRequest request,
            List<MultipartFile> images) {
        Room room = roomRepository.findByIdAndFloorHouseLandlordId(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Post post = postRepository
                .findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new AppException(ErrorCode.ROOM_UPDATE_NOT_ALLOWED);
        }

        Floor floor = floorRepository
                .findByIdAndHouseIdAndHouseLandlordId(
                        request.getFloorId(), request.getHouseId(), landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));

        room.setFloor(floor);
        room.setRoomNumber(request.getRoomNumber().strip());
        room.setArea(request.getArea());
        room.setBasePrice(request.getBasePrice());
        room.setMaxTenants(request.getMaxTenants());
        room.setHasPrivateBathroom(request.isHasPrivateBathroom());
        room.setHasAirConditioner(request.isHasAirConditioner());
        room.setHasWaterHeater(request.isHasWaterHeater());
        room.setHasBalcony(request.isHasBalcony());
        room.setAmenitiesDescription(request.getAmenitiesDescription());

        post.setTitle(request.getTitle().strip());
        post.setDescription(request.getDescription().strip());
        post.setRentalPrice(request.getBasePrice());
        post.setDepositAmount(request.getDepositAmount());
        post.setAvailableFrom(request.getAvailableFrom());
        if (post.getStatus() == PostStatus.REJECTED) {
            post.setStatus(PostStatus.DRAFT);
            post.setRejectReason(null);
        }

        roomRepository.save(room);
        postRepository.save(post);

        List<RoomImage> existingImages = roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId);
        List<String> imageUrls = existingImages.stream()
                .map(RoomImage::getImageUrl)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<MultipartFile> newImages = images == null ? List.of() : images;
        for (int index = 0; index < newImages.size(); index++) {
            String imageUrl = imageStorageService.upload(newImages.get(index));
            imageUrls.add(imageUrl);
            roomImageRepository.save(RoomImage.builder()
                    .room(room)
                    .imageUrl(imageUrl)
                    .primary(existingImages.isEmpty() && index == 0)
                    .displayOrder(existingImages.size() + index)
                    .build());
        }

        return LandlordRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .houseId(floor.getHouse().getId())
                .floorId(floor.getId())
                .roomNumber(room.getRoomNumber())
                .area(room.getArea())
                .basePrice(room.getBasePrice())
                .roomStatus(room.getStatus())
                .postStatus(post.getStatus())
                .rented(room.isRented())
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional
    public LandlordRoomResponse submit(UUID landlordId, UUID roomId) {
        Room room = roomRepository.findByIdAndFloorHouseLandlordId(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Post post = postRepository
                .findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new AppException(ErrorCode.INVALID_POST_STATUS_TRANSITION);
        }
        if (room.getArea() == null || room.getArea().compareTo(BigDecimal.ZERO) <= 0
                || room.getBasePrice() == null || room.getBasePrice().compareTo(BigDecimal.ZERO) <= 0
                || roomImageRepository.countByRoomId(roomId) == 0) {
            throw new AppException(ErrorCode.ROOM_SUBMISSION_INCOMPLETE);
        }

        post.setStatus(PostStatus.PENDING);
        post.setRejectReason(null);
        postRepository.save(post);

        List<String> imageUrls = roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)
                .stream()
                .map(RoomImage::getImageUrl)
                .toList();
        return LandlordRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .houseId(room.getFloor().getHouse().getId())
                .floorId(room.getFloor().getId())
                .roomNumber(room.getRoomNumber())
                .area(room.getArea())
                .basePrice(room.getBasePrice())
                .roomStatus(room.getStatus())
                .postStatus(post.getStatus())
                .rented(room.isRented())
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional
    public LandlordRoomResponse hide(UUID landlordId, UUID roomId) {
        Room room = roomRepository.findByIdAndFloorHouseLandlordId(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Post post = postRepository
                .findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.APPROVED) {
            throw new AppException(ErrorCode.INVALID_POST_STATUS_TRANSITION);
        }
        post.setStatus(PostStatus.HIDDEN);
        postRepository.save(post);

        List<String> imageUrls = roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)
                .stream()
                .map(RoomImage::getImageUrl)
                .toList();
        return LandlordRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .houseId(room.getFloor().getHouse().getId())
                .floorId(room.getFloor().getId())
                .roomNumber(room.getRoomNumber())
                .area(room.getArea())
                .basePrice(room.getBasePrice())
                .roomStatus(room.getStatus())
                .postStatus(post.getStatus())
                .rented(room.isRented())
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional
    public LandlordRoomResponse unhide(UUID landlordId, UUID roomId) {
        Room room = roomRepository.findByIdAndFloorHouseLandlordId(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Post post = postRepository
                .findFirstByRoomIdAndLandlordIdOrderByCreatedAtDesc(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.HIDDEN) {
            throw new AppException(ErrorCode.INVALID_POST_STATUS_TRANSITION);
        }
        post.setStatus(PostStatus.APPROVED);
        postRepository.save(post);

        List<String> imageUrls = roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)
                .stream()
                .map(RoomImage::getImageUrl)
                .toList();
        return LandlordRoomResponse.builder()
                .roomId(room.getId())
                .postId(post.getId())
                .houseId(room.getFloor().getHouse().getId())
                .floorId(room.getFloor().getId())
                .roomNumber(room.getRoomNumber())
                .area(room.getArea())
                .basePrice(room.getBasePrice())
                .roomStatus(room.getStatus())
                .postStatus(post.getStatus())
                .rented(room.isRented())
                .imageUrls(imageUrls)
                .build();
    }
}
