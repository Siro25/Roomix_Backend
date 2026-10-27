package com.team4.core.services;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.LandlordPropertyResponse;
import java.util.List;
import java.util.UUID;

public interface LandlordPropertyService {
    HouseResponse createHouse(UUID landlordId, HouseRequest request);

    List<HouseResponse> getHouses(UUID landlordId);

    List<LandlordPropertyResponse> getOverview(UUID landlordId);

    HouseResponse getHouse(UUID landlordId, UUID houseId);


    HouseResponse updateHouse(UUID landlordId, UUID houseId, HouseRequest request);

    void deleteHouse(UUID landlordId, UUID houseId);

    FloorResponse createFloor(UUID landlordId, UUID houseId, FloorRequest request);

    List<FloorResponse> getFloors(UUID landlordId, UUID houseId);

    FloorResponse updateFloor(UUID landlordId, UUID houseId, UUID floorId, FloorRequest request);

    void deleteFloor(UUID landlordId, UUID houseId, UUID floorId);
}
