package com.team4.core.services;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "debug=false",
        "spring.datasource.url=jdbc:h2:mem:houses;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.main.banner-mode=off"
})
@Transactional
class HousePersistenceTest {
    @Autowired HouseService service;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    @Test
    void migratesAndPersistsCrudWithOwnerScopedSearch() {
        UUID owner = landlord();
        UUID other = landlord();
        var first = service.create(owner, request("Nhà 100%"));
        service.create(owner, request("Nhà 100A"));
        service.create(other, request("Nhà 100%"));
        assertThat(first.id()).isNotNull();
        assertThat(first.createdAt()).isNotNull();
        assertThat(first.updatedAt()).isNotNull();
        var filtered = service.list(owner, "%", "TP C", "Phường A", 0, 20, "name,asc");
        assertThat(filtered.totalElements()).isEqualTo(1);
        assertThat(filtered.items().getFirst().id()).isEqualTo(first.id());
        var page = service.list(owner, null, null, null, 0, 1, "createdAt,desc");
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items()).hasSize(1);
        var patch = new HouseRequest();
        patch.setName("Nhà mới");
        service.update(owner, first.id(), patch);
        assertThat(service.get(owner, first.id()).name()).isEqualTo("Nhà mới");
        assertThatThrownBy(() -> service.get(other, first.id())).isInstanceOf(AppException.class);
        service.delete(owner, first.id());
        assertThat(jdbc.queryForObject("select count(*) from houses where id = ?", Integer.class, first.id())).isZero();
    }

    @Test
    void persistedFloorCountBlocksDeletion() {
        UUID owner = landlord();
        var house = service.create(owner, request("Nhà A"));
        jdbc.update("update houses set total_floors = 1 where id = ?", house.id());
        // Clear the persistence context so the service reads the updated database value.
        entityManager.clear();
        assertThatThrownBy(() -> service.delete(owner, house.id()))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.HOUSE_HAS_FLOORS));
    }

    @Autowired jakarta.persistence.EntityManager entityManager;

    private UUID landlord() {
        String unique = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().username(unique).email(unique + "@example.com")
                .passwordHash("test-only").fullName("Chủ trọ").phoneNumber("0900000000")
                .role(Role.LANDLORD).status(UserStatus.ACTIVE).build()).getId();
    }

    private HouseRequest request(String name) {
        var request = new HouseRequest();
        request.setName(name);
        request.setAddressStreet("12 A");
        request.setWard("Phường A");
        request.setCity("TP C");
        return request;
    }
}
