package com.team4.core.services.impl;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.entities.House;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.repositories.specifications.HouseSpecifications;
import com.team4.core.services.HouseService;
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
public class HouseServiceImpl implements HouseService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name");
    private final HouseRepository houses;
    private final UserRepository users;
    private final Validator validator;

    @Override
    @Transactional
    public HouseResponse create(UUID landlordId, HouseRequest request) {
        validate(request);
        var house = new House(users.getReferenceById(landlordId));
        apply(house, request);
        return HouseResponse.from(houses.saveAndFlush(house));
    }

    @Override
    public PageResponse<HouseResponse> list(UUID landlordId, String keyword, String city, String ward,
            int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        var pageable = PageRequest.of(page, size, parseSort(sort));
        return PageResponse.from(houses.findAll(
                HouseSpecifications.ownedBy(landlordId, keyword, city, ward), pageable)
                .map(HouseResponse::from));
    }

    @Override
    public HouseResponse get(UUID landlordId, UUID houseId) {
        return HouseResponse.from(houses.findByIdAndLandlord_Id(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND)));
    }

    @Override
    @Transactional
    public HouseResponse update(UUID landlordId, UUID houseId, HouseRequest request) {
        if (request.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        House house = ownedForUpdate(landlordId, houseId);
        HouseRequest merged = merge(house, request);
        validate(merged);
        apply(house, merged);
        houses.flush();
        return HouseResponse.from(house);
    }

    @Override
    @Transactional
    public void delete(UUID landlordId, UUID houseId) {
        House house = ownedForUpdate(landlordId, houseId);
        if (house.getTotalFloors() > 0) {
            throw new AppException(ErrorCode.HOUSE_HAS_FLOORS);
        }
        try {
            houses.delete(house);
            houses.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.HOUSE_HAS_DEPENDENCIES);
        }
    }

    private House ownedForUpdate(UUID landlordId, UUID houseId) {
        return houses.findOwnedForUpdate(houseId, landlordId)
                .orElseThrow(() -> new AppException(ErrorCode.HOUSE_NOT_FOUND));
    }

    private void validate(HouseRequest request) {
        if (!validator.validate(request).isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Sort parseSort(String value) {
        String[] parts = value == null ? new String[0] : value.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc"))) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]).and(Sort.by("id"));
    }

    private void apply(House house, HouseRequest request) {
        house.updateDetails(request.getName(), request.getAddressStreet(), request.getWard(),
                request.getDistrict(), request.getCity(), request.getLatitude(), request.getLongitude(),
                request.getDescription());
    }

    private HouseRequest merge(House house, HouseRequest patch) {
        var merged = new HouseRequest();
        merged.setName(patch.hasField("name") ? patch.getName() : house.getName());
        merged.setAddressStreet(patch.hasField("addressStreet") ? patch.getAddressStreet() : house.getAddressStreet());
        merged.setWard(patch.hasField("ward") ? patch.getWard() : house.getWard());
        merged.setDistrict(patch.hasField("district") ? patch.getDistrict() : house.getDistrict());
        merged.setCity(patch.hasField("city") ? patch.getCity() : house.getCity());
        merged.setLatitude(patch.hasField("latitude") ? patch.getLatitude() : house.getLatitude());
        merged.setLongitude(patch.hasField("longitude") ? patch.getLongitude() : house.getLongitude());
        merged.setDescription(patch.hasField("description") ? patch.getDescription() : house.getDescription());
        return merged;
    }
}
