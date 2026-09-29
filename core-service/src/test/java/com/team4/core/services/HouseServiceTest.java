package com.team4.core.services;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.entities.House;
import com.team4.core.entities.User;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.HouseRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.impl.HouseServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HouseServiceTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID houseId = UUID.randomUUID();
    private final HouseRepository houses = mock(HouseRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private ValidatorFactory validation;
    private HouseServiceImpl service;
    private House house;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        validation = Validation.buildDefaultValidatorFactory();
        service = new HouseServiceImpl(houses, users, validation.getValidator());
        var user = User.builder().id(owner).build();
        house = new House(user);
        ReflectionTestUtils.setField(house, "id", houseId);
        house.updateDetails("Nhà A", "12 đường A", "Phường A", "Khu B", "TP C",
                new BigDecimal("10"), new BigDecimal("106"), "Mô tả");
        when(houses.findOwnedForUpdate(houseId, owner)).thenReturn(Optional.of(house));
        when(users.getReferenceById(owner)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        validation.close();
    }

    @Test
    void createUsesAuthenticatedOwnerAndStartsWithNoFloors() {
        when(houses.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(owner, request("""
                {"name":"  Nhà mới  ","addressStreet":"12 A","ward":"A","city":"B"}
                """));
        assertThat(result.landlordId()).isEqualTo(owner);
        assertThat(result.name()).isEqualTo("Nhà mới");
        assertThat(result.totalFloors()).isZero();
    }

    @Test
    void patchPreservesOmittedFieldsAndClearsOptionalFields() {
        var result = service.update(owner, houseId, request("{\"name\":\" Nhà B \",\"description\":null}"));
        assertThat(result.name()).isEqualTo("Nhà B");
        assertThat(result.description()).isNull();
        assertThat(result.addressStreet()).isEqualTo("12 đường A");
        assertThat(result.latitude()).isEqualByComparingTo("10");
    }

    @Test
    void coordinatesAreValidatedAfterMerging() {
        var result = service.update(owner, houseId, request("{\"latitude\":11}"));
        assertThat(result.longitude()).isEqualByComparingTo("106");
        assertError(() -> service.update(owner, houseId, request("{\"latitude\":null}")), ErrorCode.INVALID_REQUEST);
        result = service.update(owner, houseId, request("{\"latitude\":null,\"longitude\":null}"));
        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();
    }

    @Test
    void rejectsEmptyPatchNullRequiredFieldAndOutOfRangeCoordinates() {
        for (String json : new String[]{"{}", "{\"name\":null}", "{\"name\":\"  \"}", "{\"latitude\":91}"}) {
            assertError(() -> service.update(owner, houseId, request(json)), ErrorCode.INVALID_REQUEST);
        }
        verify(houses, never()).flush();
    }

    @Test
    void rejectsUnknownAndServerManagedFields() {
        for (String field : new String[]{"landlordId", "totalFloors", "id", "createdAt", "unexpected"}) {
            assertThatThrownBy(() -> request("{\"" + field + "\":1}")).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void anotherOwnerCannotReadUpdateOrDelete() {
        UUID another = UUID.randomUUID();
        assertError(() -> service.get(another, houseId), ErrorCode.HOUSE_NOT_FOUND);
        assertError(() -> service.update(another, houseId, request("{\"name\":\"B\"}")), ErrorCode.HOUSE_NOT_FOUND);
        assertError(() -> service.delete(another, houseId), ErrorCode.HOUSE_NOT_FOUND);
        verify(houses, never()).delete(any(House.class));
    }

    @Test
    void refusesDeletingHouseWithFloors() {
        ReflectionTestUtils.setField(house, "totalFloors", 1);
        assertError(() -> service.delete(owner, houseId), ErrorCode.HOUSE_HAS_FLOORS);
        verify(houses, never()).delete(any(House.class));
    }

    @Test
    void deletesEmptyHouseAndTranslatesForeignKeyConflict() {
        service.delete(owner, houseId);
        verify(houses).delete(house);
        doThrow(new DataIntegrityViolationException("foreign key")).when(houses).flush();
        assertError(() -> service.delete(owner, houseId), ErrorCode.HOUSE_HAS_DEPENDENCIES);
    }

    @Test
    void rejectsInvalidPaginationAndSort() {
        assertError(() -> service.list(owner, null, null, null, -1, 20, "name,asc"), ErrorCode.INVALID_REQUEST);
        assertError(() -> service.list(owner, null, null, null, 0, 101, "name,asc"), ErrorCode.INVALID_REQUEST);
        assertError(() -> service.list(owner, null, null, null, 0, 20, "landlordId,asc"), ErrorCode.INVALID_REQUEST);
        assertError(() -> service.list(owner, null, null, null, 0, 20, "name,invalid"), ErrorCode.INVALID_REQUEST);
    }

    private HouseRequest request(String json) {
        return mapper.readValue(json, HouseRequest.class);
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
