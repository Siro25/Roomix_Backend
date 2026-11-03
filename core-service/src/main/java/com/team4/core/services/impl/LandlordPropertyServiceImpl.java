package com.team4.core.services.impl;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import com.team4.core.entities.Floor;
import com.team4.core.entities.House;
import com.team4.core.entities.Post;
import com.team4.core.entities.Room;
import com.team4.core.enums.RoomStatus;
import com.team4.core.enums.PostStatus;
import com.team4.core.enums.Role;
import com.team4.core.enums.NotificationType;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FloorRepository;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.repositories.RoomImageRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.AccountNotificationService;
import com.team4.core.services.LandlordPropertyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandlordPropertyServiceImpl implements LandlordPropertyService {
    private final HouseRepository houseRepository;
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;
    private final PostRepository postRepository;
    private final RoomImageRepository roomImageRepository;
    private final UserRepository userRepository;
    private final AccountNotificationService notificationService;

    @Override
    @Transactional
    public HouseResponse createHouse(UUID landlordId, HouseRequest request) {
        House house = houseRepository.save(House.builder()
                .landlordId(landlordId).name(request.getName().strip())
                .addressStreet(request.getAddressStreet().strip()).ward(request.getWard().strip())
                .district(request.getDistrict().strip()).city(request.getCity().strip())
                .latitude(request.getLatitude()).longitude(request.getLongitude())
                .description(request.getDescription() == null ? null : request.getDescription().strip())
                .totalFloors(request.getTotalFloors()).build());
        return HouseResponse.builder().id(house.getId()).name(house.getName())
                .addressStreet(house.getAddressStreet()).ward(house.getWard()).district(house.getDistrict())
                .city(house.getCity()).latitude(house.getLatitude()).longitude(house.getLongitude())
                .description(house.getDescription()).totalFloors(house.getTotalFloors())
                .createdAt(house.getCreatedAt()).updatedAt(house.getUpdatedAt()).floors(List.of()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getHouses(UUID landlordId) {
        return houseRepository.findOverviewByLandlordId(landlordId).stream()
                .map(house -> HouseResponse.builder()
                        .id(house.getId())
                        .name(house.getName())
                        .addressStreet(house.getAddressStreet())
                        .ward(house.getWard())
                        .district(house.getDistrict())
                        .city(house.getCity())
                        .latitude(house.getLatitude())
                        .longitude(house.getLongitude())
                        .description(house.getDescription())
                        .totalFloors(house.getTotalFloors())
                        .createdAt(house.getCreatedAt())
                        .updatedAt(house.getUpdatedAt())
                        .floors(house.getFloors().stream()
                                .map(floor -> FloorResponse.builder()
                                        .id(floor.getId())
                                        .houseId(house.getId())
                                        .floorNumber(floor.getFloorNumber())
                                        .name(floor.getName())
                                        .description(floor.getDescription())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordPropertyResponse> getOverview(UUID landlordId) {
        List<House> houses = houseRepository.findOverviewByLandlordId(landlordId);
        Map<UUID, List<Room>> roomsByFloor = roomRepository.findOverviewByLandlordId(landlordId)
                .stream()
                .collect(Collectors.groupingBy(
                        room -> room.getFloor().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<UUID, Post> postsByRoom = new LinkedHashMap<>();
        postRepository.findByLandlordIdOrderByCreatedAtDesc(landlordId)
                .forEach(post -> postsByRoom.putIfAbsent(post.getRoomId(), post));

        return houses.stream().map(house -> {
            List<LandlordPropertyResponse.FloorSummary> floors = house.getFloors().stream()
                    .map(floor -> {
                        List<LandlordPropertyResponse.RoomSummary> rooms = roomsByFloor
                                .getOrDefault(floor.getId(), List.of())
                                .stream()
                                .map(room -> {
                                    Post post = postsByRoom.get(room.getId());
                                    return LandlordPropertyResponse.RoomSummary.builder()
                                            .roomId(room.getId())
                                            .postId(post == null ? null : post.getId())
                                            .roomNumber(room.getRoomNumber())
                                            .title(post == null ? null : post.getTitle())
                                            .area(room.getArea())
                                            .basePrice(post == null || post.getRentalPrice() == null
                                                    ? room.getBasePrice()
                                                    : post.getRentalPrice())
                                            .maxTenants(room.getMaxTenants())
                                            .roomStatus(room.getStatus())
                                            .postStatus(post == null ? null : post.getStatus())
                                            .rejectReason(post == null ? null : post.getRejectReason())
                                            .rented(room.isRented())
                                            .build();
                                })
                                .toList();
                        int availableRooms = (int) rooms.stream()
                                .filter(room -> !room.isRented()
                                        && room.getRoomStatus() == RoomStatus.AVAILABLE)
                                .count();
                        int rentedRooms = (int) rooms.stream()
                                .filter(LandlordPropertyResponse.RoomSummary::isRented)
                                .count();
                        return LandlordPropertyResponse.FloorSummary.builder()
                                .floorId(floor.getId())
                                .floorNumber(floor.getFloorNumber())
                                .floorName(floor.getName())
                                .description(floor.getDescription())
                                .totalRooms(rooms.size())
                                .availableRooms(availableRooms)
                                .rentedRooms(rentedRooms)
                                .rooms(rooms)
                                .build();
                    })
                    .toList();
            int totalRooms = floors.stream()
                    .mapToInt(LandlordPropertyResponse.FloorSummary::getTotalRooms)
                    .sum();
            int availableRooms = floors.stream()
                    .mapToInt(LandlordPropertyResponse.FloorSummary::getAvailableRooms)
                    .sum();
            int rentedRooms = floors.stream()
                    .mapToInt(LandlordPropertyResponse.FloorSummary::getRentedRooms)
                    .sum();
            return LandlordPropertyResponse.builder()
                    .houseId(house.getId())
                    .houseName(house.getName())
                    .address(String.join(", ", house.getAddressStreet(), house.getWard(),
                            house.getDistrict(), house.getCity()))
                    .addressStreet(house.getAddressStreet())
                    .ward(house.getWard())
                    .district(house.getDistrict())
                    .city(house.getCity())
                    .latitude(house.getLatitude())
                    .longitude(house.getLongitude())
                    .description(house.getDescription())
                    .totalFloors(house.getTotalFloors())
                    .configuredFloors(floors.size())
                    .totalRooms(totalRooms)
                    .availableRooms(availableRooms)
                    .rentedRooms(rentedRooms)
                    .createdAt(house.getCreatedAt())
                    .updatedAt(house.getUpdatedAt())
                    .floors(floors)
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void submitHouseListing(UUID landlordId, UUID houseId) {
        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        List<Room> rooms = roomRepository.findOverviewByLandlordId(landlordId).stream()
                .filter(room -> room.getFloor().getHouse().getId().equals(houseId))
                .toList();
        if (rooms.isEmpty()) {
            throw new AppException(ErrorCode.ROOM_SUBMISSION_INCOMPLETE);
        }

        Map<UUID, Post> latestPostByRoom = new LinkedHashMap<>();
        postRepository.findByHouseIdAndLandlordId(houseId, landlordId)
                .forEach(post -> latestPostByRoom.putIfAbsent(post.getRoomId(), post));
        if (latestPostByRoom.size() != rooms.size()) {
            throw new AppException(ErrorCode.ROOM_SUBMISSION_INCOMPLETE);
        }

        for (Room room : rooms) {
            Post post = latestPostByRoom.get(room.getId());
            if (post == null
                    || (post.getStatus() != PostStatus.DRAFT
                    && post.getStatus() != PostStatus.REJECTED
                    && post.getStatus() != PostStatus.HIDDEN
                    && post.getStatus() != PostStatus.APPROVED)
                    || room.getArea() == null
                    || room.getArea().signum() <= 0
                    || room.getBasePrice() == null
                    || room.getBasePrice().signum() <= 0
                    || roomImageRepository.countByRoomId(room.getId()) == 0) {
                throw new AppException(ErrorCode.ROOM_SUBMISSION_INCOMPLETE);
            }
        }

        latestPostByRoom.values().forEach(post -> {
            post.setStatus(PostStatus.PENDING);
            post.setRejectReason(null);
        });
        postRepository.saveAll(latestPostByRoom.values());
        userRepository.findAllByRole(Role.ADMIN).forEach(admin -> notificationService.send(
                admin.getId(),
                houseId,
                NotificationType.POST_APPROVAL,
                "HOUSE_LISTING_SUBMITTED",
                "Có tin nhà mới chờ duyệt",
                "Nhà \"" + house.getName() + "\" cùng " + rooms.size() + " phòng vừa được gửi duyệt.",
                "/admin/moderation"));
    }

    @Override
    @Transactional(readOnly = true)
    public HouseResponse getHouse(UUID landlordId, UUID houseId) {

        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        List<FloorResponse> floors = floorRepository.findByHouseIdAndHouseLandlordIdOrderByFloorNumberAsc(
                houseId, landlordId).stream().map(floor -> FloorResponse.builder().id(floor.getId())
                        .houseId(houseId).floorNumber(floor.getFloorNumber()).name(floor.getName())
                        .description(floor.getDescription()).build()).toList();
        return HouseResponse.builder().id(house.getId()).name(house.getName())
                .addressStreet(house.getAddressStreet()).ward(house.getWard()).district(house.getDistrict())
                .city(house.getCity()).latitude(house.getLatitude()).longitude(house.getLongitude())
                .description(house.getDescription()).totalFloors(house.getTotalFloors())
                .createdAt(house.getCreatedAt()).updatedAt(house.getUpdatedAt()).floors(floors).build();
    }

    @Override
    @Transactional
    public HouseResponse updateHouse(UUID landlordId, UUID houseId, HouseRequest request) {
        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        if (floorRepository.existsByHouseIdAndFloorNumberGreaterThan(houseId, request.getTotalFloors())) {
            throw new AppException(ErrorCode.HOUSE_UPDATE_NOT_ALLOWED);
        }
        house.setName(request.getName().strip());
        house.setAddressStreet(request.getAddressStreet().strip());
        house.setWard(request.getWard().strip());
        house.setDistrict(request.getDistrict().strip());
        house.setCity(request.getCity().strip());
        house.setLatitude(request.getLatitude());
        house.setLongitude(request.getLongitude());
        house.setDescription(request.getDescription() == null ? null : request.getDescription().strip());
        house.setTotalFloors(request.getTotalFloors());
        houseRepository.save(house);
        return HouseResponse.builder().id(house.getId()).name(house.getName())
                .addressStreet(house.getAddressStreet()).ward(house.getWard()).district(house.getDistrict())
                .city(house.getCity()).latitude(house.getLatitude()).longitude(house.getLongitude())
                .description(house.getDescription()).totalFloors(house.getTotalFloors())
                .createdAt(house.getCreatedAt()).updatedAt(house.getUpdatedAt()).floors(List.of()).build();
    }

    @Override
    @Transactional
    public void deleteHouse(UUID landlordId, UUID houseId) {
        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        if (roomRepository.existsByFloorHouseId(houseId)) {
            throw new AppException(ErrorCode.HOUSE_DELETE_NOT_ALLOWED);
        }
        houseRepository.delete(house);
    }

    @Override
    @Transactional
    public FloorResponse createFloor(UUID landlordId, UUID houseId, FloorRequest request) {
        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        if (request.getFloorNumber() > house.getTotalFloors()) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_INVALID);
        }
        if (floorRepository.existsByHouseIdAndFloorNumber(houseId, request.getFloorNumber())) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_EXISTS);
        }
        Floor floor = floorRepository.save(Floor.builder().house(house).floorNumber(request.getFloorNumber())
                .name("Tầng " + request.getFloorNumber())
                .description(request.getDescription() == null ? null : request.getDescription().strip()).build());
        return FloorResponse.builder().id(floor.getId()).houseId(houseId).floorNumber(floor.getFloorNumber())
                .name(floor.getName()).description(floor.getDescription()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorResponse> getFloors(UUID landlordId, UUID houseId) {
        houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        return floorRepository.findByHouseIdAndHouseLandlordIdOrderByFloorNumberAsc(houseId, landlordId)
                .stream().map(floor -> FloorResponse.builder().id(floor.getId()).houseId(houseId)
                        .floorNumber(floor.getFloorNumber()).name(floor.getName())
                        .description(floor.getDescription()).build()).toList();
    }

    @Override
    @Transactional
    public FloorResponse updateFloor(UUID landlordId, UUID houseId, UUID floorId, FloorRequest request) {
        House house = houseRepository.findByIdAndLandlordId(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        Floor floor = floorRepository.findByIdAndHouseIdAndHouseLandlordId(floorId, houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
        if (request.getFloorNumber() > house.getTotalFloors()) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_INVALID);
        }
        if (floorRepository.existsByHouseIdAndFloorNumberAndIdNot(houseId, request.getFloorNumber(), floorId)) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_EXISTS);
        }
        floor.setFloorNumber(request.getFloorNumber());
        floor.setName("Tầng " + request.getFloorNumber());
        floor.setDescription(request.getDescription() == null ? null : request.getDescription().strip());
        floorRepository.save(floor);
        return FloorResponse.builder().id(floor.getId()).houseId(houseId).floorNumber(floor.getFloorNumber())
                .name(floor.getName()).description(floor.getDescription()).build();
    }

    @Override
    @Transactional
    public void deleteFloor(UUID landlordId, UUID houseId, UUID floorId) {
        Floor floor = floorRepository.findByIdAndHouseIdAndHouseLandlordId(floorId, houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
        if (roomRepository.existsByFloorId(floorId)) {
            throw new AppException(ErrorCode.FLOOR_DELETE_NOT_ALLOWED);
        }
        floorRepository.delete(floor);
    }
}
