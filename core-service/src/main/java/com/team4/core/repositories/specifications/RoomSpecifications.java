package com.team4.core.repositories.specifications;

import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.entities.Room;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class RoomSpecifications {
    private RoomSpecifications() {}

    public static Specification<Room> ownedBy(UUID landlordId, RoomSearchRequest filter) {
        return (root, query, cb) -> {
            var house = root.get("floor").get("house");
            var predicate = cb.equal(house.get("landlord").get("id"), landlordId);
            if (filter.getHouseId() != null) {
                predicate = cb.and(predicate, cb.equal(house.get("id"), filter.getHouseId()));
            }
            if (filter.getFloorId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("floor").get("id"), filter.getFloorId()));
            }
            if (filter.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getMinPrice() != null) {
                predicate = cb.and(predicate, cb.ge(root.get("basePrice"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicate = cb.and(predicate, cb.le(root.get("basePrice"), filter.getMaxPrice()));
            }
            if (StringUtils.hasText(filter.getKeyword())) {
                String keyword = filter.getKeyword().strip().toUpperCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicate = cb.and(predicate, cb.like(root.get("roomNumber"), "%" + keyword + "%", '!'));
            }
            return predicate;
        };
    }
}
