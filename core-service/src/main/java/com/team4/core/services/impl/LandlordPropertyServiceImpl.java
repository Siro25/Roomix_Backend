package com.team4.core.services.impl;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import com.team4.core.entities.Floor;

import com.team4.core.entities.House;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.FloorRepository;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.services.LandlordPropertyService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LandlordPropertyServiceImpl implements LandlordPropertyService {
    private final HouseRepository houseRepository;
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;

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
        return houseRepository.findDistinctByLandlordIdOrderByCreatedAtDesc(landlordId).stream()
                .map(house -> HouseResponse.builder().id(house.getId()).name(house.getName())
                        .addressStreet(house.getAddressStreet()).ward(house.getWard())
                        .district(house.getDistrict()).city(house.getCity()).latitude(house.getLatitude())
                        .longitude(house.getLongitude()).description(house.getDescription())
                        .totalFloors(house.getTotalFloors()).createdAt(house.getCreatedAt())
                        .updatedAt(house.getUpdatedAt()).floors(List.of()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandlordPropertyResponse> getOverview(UUID landlordId) {
        return houseRepository.findDistinctByLandlordIdOrderByCreatedAtDesc(landlordId).stream()
                .map(LandlordPropertyResponse::from)
                .toList();
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
                .name(request.getName().strip())
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
        floor.setName(request.getName().strip());
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
