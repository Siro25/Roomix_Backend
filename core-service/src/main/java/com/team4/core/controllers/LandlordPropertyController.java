package com.team4.core.controllers;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.FloorResponse;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.services.LandlordPropertyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlords/houses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordPropertyController {
    private final LandlordPropertyService landlordPropertyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HouseResponse> createHouse(@Valid @RequestBody HouseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<HouseResponse>builder()
                .message("Tạo nhà thành công")
                .data(landlordPropertyService.createHouse(UUID.fromString(jwt.getSubject()), request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<HouseResponse>> getHouses(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<HouseResponse>>builder()
                .message("Lấy danh sách nhà thành công")
                .data(landlordPropertyService.getHouses(UUID.fromString(jwt.getSubject())))
                .build();
    }

    @GetMapping("/{houseId}")
    public ApiResponse<HouseResponse> getHouse(@PathVariable UUID houseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<HouseResponse>builder()
                .message("Lấy chi tiết nhà thành công")
                .data(landlordPropertyService.getHouse(UUID.fromString(jwt.getSubject()), houseId))
                .build();
    }

    @PutMapping("/{houseId}")
    public ApiResponse<HouseResponse> updateHouse(@PathVariable UUID houseId,
            @Valid @RequestBody HouseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<HouseResponse>builder()
                .message("Cập nhật nhà thành công")
                .data(landlordPropertyService.updateHouse(UUID.fromString(jwt.getSubject()), houseId, request))
                .build();
    }

    @DeleteMapping("/{houseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHouse(@PathVariable UUID houseId, @AuthenticationPrincipal Jwt jwt) {
        landlordPropertyService.deleteHouse(UUID.fromString(jwt.getSubject()), houseId);
    }

    @PostMapping("/{houseId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FloorResponse> createFloor(@PathVariable UUID houseId,
            @Valid @RequestBody FloorRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<FloorResponse>builder()
                .message("Tạo tầng thành công")
                .data(landlordPropertyService.createFloor(UUID.fromString(jwt.getSubject()), houseId, request))
                .build();
    }

    @GetMapping("/{houseId}/floors")
    public ApiResponse<List<FloorResponse>> getFloors(@PathVariable UUID houseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<FloorResponse>>builder()
                .message("Lấy danh sách tầng thành công")
                .data(landlordPropertyService.getFloors(UUID.fromString(jwt.getSubject()), houseId))
                .build();
    }

    @PutMapping("/{houseId}/floors/{floorId}")
    public ApiResponse<FloorResponse> updateFloor(@PathVariable UUID houseId,
            @PathVariable UUID floorId, @Valid @RequestBody FloorRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<FloorResponse>builder()
                .message("Cập nhật tầng thành công")
                .data(landlordPropertyService.updateFloor(
                        UUID.fromString(jwt.getSubject()), houseId, floorId, request))
                .build();
    }

    @DeleteMapping("/{houseId}/floors/{floorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFloor(@PathVariable UUID houseId, @PathVariable UUID floorId,
            @AuthenticationPrincipal Jwt jwt) {
        landlordPropertyService.deleteFloor(UUID.fromString(jwt.getSubject()), houseId, floorId);
    }
}
