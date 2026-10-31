package com.team4.core.services.impl;

import com.team4.core.dtos.response.CoreLandlordDashboardResponse;
import com.team4.core.enums.RoomStatus;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.RoomRepository;
import com.team4.core.services.CoreLandlordDashboardService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoreLandlordDashboardServiceImpl implements CoreLandlordDashboardService {
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public CoreLandlordDashboardResponse getPropertySummary(UUID landlordId) {
        return CoreLandlordDashboardResponse.builder()
                .totalHouses(houseRepository.countByLandlordId(landlordId))
                .totalRooms(roomRepository.countByFloorHouseLandlordId(landlordId))
                .availableRooms(roomRepository.countByFloorHouseLandlordIdAndStatusAndRentedFalse(
                        landlordId,
                        RoomStatus.AVAILABLE))
                .occupiedRooms(roomRepository.countByFloorHouseLandlordIdAndStatus(
                        landlordId,
                        RoomStatus.OCCUPIED))
                .reservedRooms(roomRepository.countByFloorHouseLandlordIdAndStatus(
                        landlordId,
                        RoomStatus.RESERVED))
                .maintenanceRooms(roomRepository.countByFloorHouseLandlordIdAndStatus(
                        landlordId,
                        RoomStatus.MAINTENANCE))
                .build();
    }
}
