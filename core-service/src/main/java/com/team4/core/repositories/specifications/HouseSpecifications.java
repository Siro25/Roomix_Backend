package com.team4.core.repositories.specifications;

import com.team4.core.entities.House;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class HouseSpecifications {
    private HouseSpecifications() {}

    public static Specification<House> ownedBy(UUID landlordId, String keyword, String city, String ward) {
        return (root, query, cb) -> {
            var predicate = cb.equal(root.get("landlord").get("id"), landlordId);
            if (StringUtils.hasText(keyword)) {
                String escaped = keyword.strip().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String pattern = "%" + escaped + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.like(cb.lower(root.get("addressStreet")), pattern, '!')));
            }
            if (StringUtils.hasText(city)) {
                predicate = cb.and(predicate, cb.equal(root.get("city"), city.strip()));
            }
            if (StringUtils.hasText(ward)) {
                predicate = cb.and(predicate, cb.equal(root.get("ward"), ward.strip()));
            }
            return predicate;
        };
    }
}
