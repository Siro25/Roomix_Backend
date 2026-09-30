package com.team4.core.services.impl;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.Floor;
import com.team4.core.entities.House;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FloorRepository;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.services.FloorService;
import jakarta.validation.Validator;
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
public class FloorServiceImpl implements FloorService {
    private static final Set<String> SORT_FIELDS = Set.of("floorNumber", "name");
    private final FloorRepository floors;
    private final HouseRepository houses;
    private final Validator validator;

    @Override
    @Transactional
    public FloorResponse create(UUID landlordId, UUID houseId, FloorRequest request) {
        validate(request);
        House house = houses.findOwnedForUpdate(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        if (floors.existsByHouse_IdAndFloorNumber(houseId, request.getFloorNumber())) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_EXISTED);
        }
        var floor = new Floor(house, request.getFloorNumber(), request.getName(), request.getDescription());
        floors.saveAndFlush(floor);
        synchronizeFloorCount(house);
        return FloorResponse.from(floor);
    }

    @Override
    public PageResponse<FloorResponse> list(UUID landlordId, UUID houseId, int page, int size, String sort) {
        var pageable = pageRequest(page, size, sort);
        houses.findByIdAndLandlord_Id(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
        return PageResponse.from(floors.findByHouse_Id(houseId, pageable).map(FloorResponse::from));
    }

    @Override
    public FloorResponse get(UUID landlordId, UUID floorId) {
        return FloorResponse.from(ownedFloor(landlordId, floorId));
    }

    @Override
    @Transactional
    public FloorResponse update(UUID landlordId, UUID floorId, FloorRequest request) {
        if (request.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        lockHouseForFloor(landlordId, floorId);
        Floor floor = ownedFloor(landlordId, floorId);
        FloorRequest merged = merge(floor, request);
        validate(merged);
        if (floors.existsByHouse_IdAndFloorNumberAndIdNot(
                floor.getHouse().getId(), merged.getFloorNumber(), floorId)) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_EXISTED);
        }
        floor.updateDetails(merged.getFloorNumber(), merged.getName(), merged.getDescription());
        floors.flush();
        return FloorResponse.from(floor);
    }

    @Override
    @Transactional
    public void delete(UUID landlordId, UUID floorId) {
        House house = lockHouseForFloor(landlordId, floorId);
        Floor floor = ownedFloor(landlordId, floorId);
        try {
            // The room module must reference floors with ON DELETE RESTRICT.
            floors.delete(floor);
            floors.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.FLOOR_HAS_ROOMS);
        }
        synchronizeFloorCount(house);
    }

    private Floor ownedFloor(UUID landlordId, UUID floorId) {
        return floors.findByIdAndHouse_Landlord_Id(floorId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
    }

    /** All floor mutations lock the house first, also coordinating with house deletion. */
    private House lockHouseForFloor(UUID landlordId, UUID floorId) {
        UUID houseId = floors.findOwnedHouseId(floorId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
        return houses.findOwnedForUpdate(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
    }

    private void synchronizeFloorCount(House house) {
        house.synchronizeFloorCount(Math.toIntExact(floors.countByHouse_Id(house.getId())));
        houses.flush();
    }

    private void validate(FloorRequest request) {
        if (!validator.validate(request).isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private FloorRequest merge(Floor floor, FloorRequest patch) {
        var merged = new FloorRequest();
        merged.setFloorNumber(patch.hasField("floorNumber") ? patch.getFloorNumber() : Integer.valueOf(floor.getFloorNumber()));
        merged.setName(patch.hasField("name") ? patch.getName() : floor.getName());
        merged.setDescription(patch.hasField("description") ? patch.getDescription() : floor.getDescription());
        return merged;
    }

    private PageRequest pageRequest(int page, int size, String sort) {
        String[] parts = sort == null ? new String[0] : sort.split(",", -1);
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE || parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc"))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(parts[1]), parts[0]).and(Sort.by("id")));
    }
}
