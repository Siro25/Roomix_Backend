package com.team4.core.services.impl;

import com.team4.core.dtos.request.RoomRequest;
import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomResponse;
import com.team4.core.entities.Floor;
import com.team4.core.entities.Room;
import com.team4.core.enums.RoomStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FloorRepository;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.repositories.specifications.RoomSpecifications;
import com.team4.core.services.RoomService;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {
    private static final Set<String> SORT_FIELDS = Set.of("roomNumber", "basePrice", "area", "createdAt");
    private final RoomRepository rooms;
    private final FloorRepository floors;
    private final HouseRepository houses;
    private final Validator validator;

    @Override
    @Transactional
    public RoomResponse create(UUID landlordId, UUID floorId, RoomRequest request) {
        validate(request);
        UUID houseId = floors.findOwnedHouseId(floorId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
        lockHouse(landlordId, houseId, ErrorCode.FLOOR_NOT_FOUND);
        Floor floor = ownedFloor(landlordId, floorId);
        if (rooms.existsByFloor_IdAndRoomNumber(floorId, request.getRoomNumber())) {
            throw new AppException(ErrorCode.ROOM_NUMBER_EXISTED);
        }
        var room = new Room(floor);
        apply(room, request);
        return RoomResponse.from(rooms.saveAndFlush(room));
    }

    @Override
    public PageResponse<RoomResponse> list(UUID landlordId, RoomSearchRequest request) {
        var pageable = pageRequest(request);
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        if (request.getHouseId() != null) {
            houses.findByIdAndLandlord_Id(request.getHouseId(), landlordId)
                    .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        }
        if (request.getFloorId() != null) {
            Floor floor = ownedFloor(landlordId, request.getFloorId());
            if (request.getHouseId() != null && !request.getHouseId().equals(floor.getHouse().getId())) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }
        return PageResponse.from(rooms.findAll(RoomSpecifications.ownedBy(landlordId, request), pageable)
                .map(RoomResponse::from));
    }

    @Override
    public RoomResponse get(UUID landlordId, UUID roomId) {
        return RoomResponse.from(ownedRoom(landlordId, roomId));
    }

    @Override
    @Transactional
    public RoomResponse update(UUID landlordId, UUID roomId, RoomRequest request) {
        if (request.isEmpty()) throw new AppException(ErrorCode.INVALID_REQUEST);
        Room room = ownedRoomForUpdate(landlordId, roomId);
        RoomRequest merged = merge(room, request);
        validate(merged);
        if (rooms.existsByFloor_IdAndRoomNumberAndIdNot(room.getFloor().getId(), merged.getRoomNumber(), roomId)) {
            throw new AppException(ErrorCode.ROOM_NUMBER_EXISTED);
        }
        if (merged.getMaxTenants() < room.getMaxTenants()
                && (room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.RESERVED)) {
            // Rental is not connected yet; do not reduce capacity below an unknown occupancy.
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        apply(room, merged);
        rooms.flush();
        return RoomResponse.from(room);
    }

    @Override
    @Transactional
    public void delete(UUID landlordId, UUID roomId) {
        Room room = ownedRoomForUpdate(landlordId, roomId);
        if (room.getStatus() != RoomStatus.AVAILABLE && room.getStatus() != RoomStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.ROOM_IN_USE);
        }
        // Core-only deletion in this phase; Rental history checks must precede enabling tenancy integration.
        try {
            rooms.delete(room);
            rooms.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ROOM_HAS_DEPENDENCIES);
        }
    }

    private Floor ownedFloor(UUID landlordId, UUID floorId) {
        return floors.findByIdAndHouse_Landlord_Id(floorId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
    }

    private Room ownedRoom(UUID landlordId, UUID roomId) {
        return rooms.findByIdAndFloor_House_Landlord_Id(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Room ownedRoomForUpdate(UUID landlordId, UUID roomId) {
        UUID houseId = rooms.findOwnedHouseId(roomId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        // Same parent-first lock order as House/Floor; re-read only after acquiring the lock.
        lockHouse(landlordId, houseId, ErrorCode.ROOM_NOT_FOUND);
        return ownedRoom(landlordId, roomId);
    }

    private void lockHouse(UUID landlordId, UUID houseId, ErrorCode notFound) {
        houses.findOwnedForUpdate(houseId, landlordId).orElseThrow(() -> new AppException(notFound));
    }

    private void validate(RoomRequest request) {
        if (!validator.validate(request).isEmpty()) throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    private void apply(Room room, RoomRequest request) {
        room.updateDetails(request.getRoomNumber(), request.getArea(), request.getBasePrice(), request.getMaxTenants(),
                request.getHasPrivateBathroom(), request.getHasAirConditioner(), request.getHasWaterHeater(),
                request.getHasBalcony(), request.getAmenitiesDescription());
    }

    private RoomRequest merge(Room room, RoomRequest patch) {
        var merged = new RoomRequest();
        merged.setRoomNumber(patch.hasField("roomNumber") ? patch.getRoomNumber() : room.getRoomNumber());
        merged.setArea(patch.hasField("area") ? patch.getArea() : room.getArea());
        merged.setBasePrice(patch.hasField("basePrice") ? patch.getBasePrice() : room.getBasePrice());
        merged.setMaxTenants(patch.hasField("maxTenants") ? patch.getMaxTenants() : Integer.valueOf(room.getMaxTenants()));
        merged.setHasPrivateBathroom(patch.hasField("hasPrivateBathroom") ? patch.getHasPrivateBathroom() : Boolean.valueOf(room.isHasPrivateBathroom()));
        merged.setHasAirConditioner(patch.hasField("hasAirConditioner") ? patch.getHasAirConditioner() : Boolean.valueOf(room.isHasAirConditioner()));
        merged.setHasWaterHeater(patch.hasField("hasWaterHeater") ? patch.getHasWaterHeater() : Boolean.valueOf(room.isHasWaterHeater()));
        merged.setHasBalcony(patch.hasField("hasBalcony") ? patch.getHasBalcony() : Boolean.valueOf(room.isHasBalcony()));
        merged.setAmenitiesDescription(patch.hasField("amenitiesDescription") ? patch.getAmenitiesDescription() : room.getAmenitiesDescription());
        return merged;
    }

    private PageRequest pageRequest(RoomSearchRequest request) {
        String[] sort = request.getSort() == null ? new String[0] : request.getSort().split(",", -1);
        if (request.getPage() < 0 || request.getSize() < 1 || request.getSize() > 100
                || (long) request.getPage() * request.getSize() > Integer.MAX_VALUE
                || sort.length != 2 || !SORT_FIELDS.contains(sort[0])
                || !(sort[1].equals("asc") || sort[1].equals("desc"))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.fromString(sort[1]), sort[0]).and(Sort.by("id")));
    }

    private void validatePriceRange(BigDecimal min, BigDecimal max) {
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0)
                || (min != null && max != null && min.compareTo(max) > 0)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
}
