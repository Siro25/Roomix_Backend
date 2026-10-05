package com.team4.core.services.impl;

import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomDetailResponse;
import com.team4.core.dtos.response.RoomSummaryResponse;
import com.team4.core.entities.Floor;
import com.team4.core.entities.House;
import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.entities.RoomImage;
import com.team4.core.enums.PostStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomImageRepository;
import com.team4.core.services.RoomQueryService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomQueryServiceImpl implements RoomQueryService {
    private final PostRepository postRepository;
    private final RoomImageRepository roomImageRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> search(RoomSearchRequest request) {
        if (request.getMinPrice() != null
                && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new AppException(ErrorCode.INVALID_PRICE_RANGE);
        }

        Sort sort = switch (request.getSort().strip().toLowerCase(Locale.ROOT)) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "rentalPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "rentalPrice");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> throw new AppException(ErrorCode.INVALID_ROOM_SORT);
        };

        Specification<Post> specification = (root, query, criteriaBuilder) -> {
            Join<Post, Room> room = root.join("room");
            Join<Room, Floor> floor = room.join("floor");
            Join<Floor, House> house = floor.join("house");
            List<Predicate> predicates = new ArrayList<>();

            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("rentalPrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("rentalPrice"), request.getMaxPrice()));
            }
            if (request.getMinArea() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        room.get("area"), request.getMinArea()));
            }
            if (request.getLocation() != null && !request.getLocation().isBlank()) {
                String location = "%" + request.getLocation().strip().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(house.get("addressStreet")), location),
                        criteriaBuilder.like(criteriaBuilder.lower(house.get("ward")), location),
                        criteriaBuilder.like(criteriaBuilder.lower(house.get("district")), location),
                        criteriaBuilder.like(criteriaBuilder.lower(house.get("city")), location)));
            }

            List<String> amenities = request.getAmenities() == null
                    ? List.of()
                    : request.getAmenities();
            for (String rawAmenity : amenities) {
                if (rawAmenity == null || rawAmenity.isBlank()) {
                    continue;
                }
                String amenity = rawAmenity.strip().toLowerCase(Locale.ROOT);
                switch (amenity) {
                    case "private_bathroom", "bathroom", "khep_kin" ->
                            predicates.add(criteriaBuilder.isTrue(room.get("hasPrivateBathroom")));
                    case "air_conditioner", "air-conditioner", "dieu_hoa" ->
                            predicates.add(criteriaBuilder.isTrue(room.get("hasAirConditioner")));
                    case "water_heater", "hot_water", "nong_lanh" ->
                            predicates.add(criteriaBuilder.isTrue(room.get("hasWaterHeater")));
                    case "balcony", "ban_cong" ->
                            predicates.add(criteriaBuilder.isTrue(room.get("hasBalcony")));
                    default -> predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(room.get("amenitiesDescription")),
                            "%" + amenity + "%"));
                }
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Page<Post> posts = postRepository.findPublicRooms(specification, pageable);
        List<UUID> roomIds = posts.getContent().stream()
                .map(Post::getRoomId)
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

        return PageResponse.from(posts, post -> {
            Room room = post.getRoom();
            House house = room.getFloor().getHouse();
            BigDecimal rentalPrice = post.getRentalPrice() == null
                    ? room.getBasePrice()
                    : post.getRentalPrice();
            return RoomSummaryResponse.builder()
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
                    .available(true)
                    .createdAt(post.getCreatedAt())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDetailResponse getDetail(UUID roomId) {
        Post post = postRepository.findFirstByRoomIdOrderByCreatedAtDesc(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_UNAVAILABLE));
        if (post.getStatus() != PostStatus.APPROVED) {
            throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
        }
        Room room = post.getRoom();
        Floor floor = room.getFloor();
        House house = floor.getHouse();
        List<String> imageUrls = roomImageRepository.findByRoomIdOrderByDisplayOrderAsc(roomId)
                .stream()
                .map(RoomImage::getImageUrl)
                .toList();
        BigDecimal rentalPrice = post.getRentalPrice() == null
                ? room.getBasePrice()
                : post.getRentalPrice();

        return RoomDetailResponse.builder()
                .postId(post.getId())
                .roomId(room.getId())
                .houseId(house.getId())
                .floorId(floor.getId())
                .houseName(house.getName())
                .floorName(floor.getName())
                .floorNumber(floor.getFloorNumber())
                .roomNumber(room.getRoomNumber())
                .title(post.getTitle())
                .description(post.getDescription())
                .rentalPrice(rentalPrice)
                .depositAmount(post.getDepositAmount())
                .area(room.getArea())
                .maxTenants(room.getMaxTenants())
                .availableFrom(post.getAvailableFrom())
                .addressStreet(house.getAddressStreet())
                .ward(house.getWard())
                .district(house.getDistrict())
                .city(house.getCity())
                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .hasAirConditioner(room.isHasAirConditioner())
                .hasWaterHeater(room.isHasWaterHeater())
                .hasBalcony(room.isHasBalcony())
                .amenitiesDescription(room.getAmenitiesDescription())
                .rented(room.isRented())
                .available(!room.isRented())
                .imageUrls(imageUrls)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
