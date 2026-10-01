package com.team4.core.rooms;

import com.team4.core.dtos.request.FloorRequest;
import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.JwtTokenProvider;
import com.team4.core.services.FloorService;
import com.team4.core.services.HouseService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP, JWT, transactions, JPA and migrations on an isolated H2 database; no mocked services. */
@SpringBootTest(properties = {
        "debug=false", "spring.main.banner-mode=off",
        "spring.datasource.url=jdbc:h2:mem:rooms;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
class RoomApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired HouseService houses;
    @Autowired FloorService floors;
    @Autowired JwtTokenProvider tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    private UUID owner;
    private UUID houseId;
    private UUID floorId;
    private String token;

    @BeforeEach
    void setUp() {
        User landlord = user(Role.LANDLORD);
        owner = landlord.getId();
        token = tokens.generateToken(landlord);
        houseId = house(owner);
        floorId = floor(owner, houseId, 0);
        // Models a future local POST/other table retaining room references.
        jdbc.execute("CREATE TABLE IF NOT EXISTS test_room_dependents (id UUID PRIMARY KEY, "
                + "room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT)");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM test_room_dependents");
        jdbc.update("DELETE FROM rooms");
        jdbc.update("DELETE FROM floors");
        jdbc.update("DELETE FROM houses");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void completeCrudNormalizesNumbersAndPreservesPatchSemantics() throws Exception {
        ObjectNode body = validBody(" p101 ").put("hasBalcony", true).put("amenitiesDescription", " Ghi chú ");
        MvcResult created = mvc.perform(auth(post(collection())).content(body.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.roomNumber").value("P101"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.floorId").value(floorId.toString()))
                .andExpect(jsonPath("$.data.houseId").value(houseId.toString()))
                .andExpect(jsonPath("$.data.hasAirConditioner").value(false))
                .andExpect(jsonPath("$.data.hasBalcony").value(true))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty()).andReturn();
        String room = id(created);
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/rooms/" + room);
        mvc.perform(auth(get("/api/rooms/" + room))).andExpect(status().isOk());
        mvc.perform(auth(patch("/api/rooms/" + room)).content("{\"basePrice\":4000000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.area").value(25.5))
                .andExpect(jsonPath("$.data.amenitiesDescription").value("Ghi chú"))
                .andExpect(jsonPath("$.data.hasBalcony").value(true));
        mvc.perform(auth(patch("/api/rooms/" + room))
                .content("{\"amenitiesDescription\":null,\"hasBalcony\":false,\"roomNumber\":\" p102 \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roomNumber").value("P102"))
                .andExpect(jsonPath("$.data.amenitiesDescription").doesNotExist())
                .andExpect(jsonPath("$.data.hasBalcony").value(false));
        mvc.perform(auth(delete("/api/rooms/" + room))).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(auth(get("/api/rooms/" + room))).andExpect(status().isNotFound());
        assertRoomCount(0);
        assertThat(houses.get(owner, houseId).totalFloors()).isEqualTo(1);
    }

    @Test
    void duplicateNumbersAreScopedToFloorAndFailedRenameRollsBack() throws Exception {
        String first = createRoom("p101");
        String second = createRoom("P102");
        mvc.perform(auth(post(collection())).content(validBody(" P101 ").toString()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2006));
        mvc.perform(auth(patch("/api/rooms/" + second)).content("{\"roomNumber\":\"p101\",\"area\":99}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(get("/api/rooms/" + second))).andExpect(jsonPath("$.data.area").value(25.5));
        mvc.perform(auth(patch("/api/rooms/" + first)).content("{\"roomNumber\":\"p101\"}"))
                .andExpect(status().isOk());
        UUID otherFloor = floor(owner, houseId, 1);
        mvc.perform(auth(post("/api/floors/" + otherFloor + "/rooms")).content(validBody("P101").toString()))
                .andExpect(status().isCreated());
        assertRoomCount(3);
    }

    @Test
    void listScopesOwnerAndCombinesFiltersWithPagination() throws Exception {
        createRoom("P101");
        mvc.perform(auth(post(collection())).content(validBody("P102").put("basePrice", 5000000).toString()))
                .andExpect(status().isCreated());
        String reserved = createRoom("P103");
        setStatus(reserved, "RESERVED");
        UUID anotherOwner = user(Role.LANDLORD).getId();
        UUID anotherFloor = floor(anotherOwner, house(anotherOwner), 0);
        // Create another owner's data using that owner's JWT, not a mocked repository.
        String anotherToken = tokens.generateToken(users.findById(anotherOwner).orElseThrow());
        mvc.perform(post("/api/floors/" + anotherFloor + "/rooms").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + anotherToken).content(validBody("P101").toString()))
                .andExpect(status().isCreated());
        mvc.perform(auth(get("/api/rooms")).param("size", "2").param("sort", "roomNumber,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2)).andExpect(jsonPath("$.data.items[0].roomNumber").value("P101"));
        mvc.perform(auth(get("/api/rooms")).param("houseId", houseId.toString()).param("floorId", floorId.toString())
                .param("status", "AVAILABLE").param("keyword", " p10 ").param("minPrice", "3500000").param("maxPrice", "3500000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].roomNumber").value("P101"));
        mvc.perform(auth(get("/api/rooms")).param("sort", "basePrice,desc").param("size", "1"))
                .andExpect(jsonPath("$.data.items[0].roomNumber").value("P102"));
        mvc.perform(auth(get("/api/rooms")).param("page", "2147483647").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void emptyListAndLiteralWildcardSearchWork() throws Exception {
        mvc.perform(auth(get("/api/rooms"))).andExpect(jsonPath("$.data.items").isEmpty());
        createRoom("P%_1"); createRoom("PAA1");
        mvc.perform(auth(get("/api/rooms")).param("keyword", "%_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void invalidOrForeignFilterIdsAreRejected() throws Exception {
        UUID other = user(Role.LANDLORD).getId();
        UUID otherHouse = house(other);
        UUID otherFloor = floor(other, otherHouse, 0);
        for (String id : List.of(otherHouse.toString(), UUID.randomUUID().toString())) {
            mvc.perform(auth(get("/api/rooms")).param("houseId", id)).andExpect(status().isNotFound());
        }
        for (String id : List.of(otherFloor.toString(), UUID.randomUUID().toString())) {
            mvc.perform(auth(get("/api/rooms")).param("floorId", id)).andExpect(status().isNotFound());
        }
        UUID ownSecondHouse = house(owner);
        mvc.perform(auth(get("/api/rooms")).param("houseId", ownSecondHouse.toString()).param("floorId", floorId.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "LIST", "GET", "PATCH", "DELETE"})
    void allEndpointsRequireAuthenticationAndLandlordRole(String operation) throws Exception {
        String room = createRoom("P101");
        mvc.perform(operation(operation, floorId.toString(), room)).andExpect(status().isUnauthorized());
        mvc.perform(operation(operation, floorId.toString(), room).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.TENANT, Role.ADMIN)) {
            mvc.perform(operation(operation, floorId.toString(), room)
                    .header("Authorization", "Bearer " + tokens.generateToken(user(role))))
                    .andExpect(status().isForbidden());
        }
        assertRoomCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET", "PATCH", "DELETE"})
    void ownershipAndMissingIdsReturn404(String operation) throws Exception {
        String room = createRoom("P101");
        mvc.perform(operation(operation, floorId.toString(), room)
                .header("Authorization", "Bearer " + tokens.generateToken(user(Role.LANDLORD))))
                .andExpect(status().isNotFound());
        mvc.perform(auth(operation(operation, UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        mvc.perform(auth(operation(operation, "bad-id", "bad-id"))).andExpect(status().isBadRequest());
        assertRoomCount(1);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("roomNumber", "null"), Arguments.of("roomNumber", "\"  \""),
                Arguments.of("roomNumber", "123"), Arguments.of("roomNumber", "true"),
                Arguments.of("roomNumber", "\"" + "A".repeat(51) + "\""),
                Arguments.of("area", "null"), Arguments.of("area", "0"), Arguments.of("area", "-1"),
                Arguments.of("area", "25.555"), Arguments.of("area", "100000000"), Arguments.of("area", "\"25\""),
                Arguments.of("basePrice", "null"), Arguments.of("basePrice", "0"), Arguments.of("basePrice", "-1"),
                Arguments.of("basePrice", "1.001"), Arguments.of("basePrice", "10000000000000"),
                Arguments.of("basePrice", "\"3500000\""), Arguments.of("basePrice", "true"),
                Arguments.of("maxTenants", "null"), Arguments.of("maxTenants", "0"), Arguments.of("maxTenants", "-1"),
                Arguments.of("maxTenants", "1.5"), Arguments.of("maxTenants", "\"2\""), Arguments.of("maxTenants", "2147483648"),
                Arguments.of("hasPrivateBathroom", "null"), Arguments.of("hasAirConditioner", "null"),
                Arguments.of("hasWaterHeater", "null"), Arguments.of("hasBalcony", "null"),
                Arguments.of("hasBalcony", "\"true\""), Arguments.of("hasBalcony", "1"),
                Arguments.of("amenitiesDescription", "123"), Arguments.of("amenitiesDescription", "[]"),
                Arguments.of("amenitiesDescription", "\"" + "A".repeat(5001) + "\""),
                Arguments.of("status", "\"OCCUPIED\""), Arguments.of("floorId", "\"other\""),
                Arguments.of("houseId", "\"other\""), Arguments.of("id", "\"other\""),
                Arguments.of("landlordId", "\"other\""), Arguments.of("createdAt", "\"date\""),
                Arguments.of("unknown", "1"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidCreateAndPatchReturn400WithoutChangingData(String field, String json) throws Exception {
        String room = createRoom("P101");
        ObjectNode body = validBody("P102");
        body.set(field, mapper.readTree(json));
        mvc.perform(auth(post(collection())).content(body.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
        mvc.perform(auth(patch("/api/rooms/" + room)).content("{\"" + field + "\":" + json + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/rooms/" + room))).andExpect(jsonPath("$.data.roomNumber").value("P101"))
                .andExpect(jsonPath("$.data.area").value(25.5));
        assertRoomCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"roomNumber", "area", "basePrice", "maxTenants"})
    void missingRequiredFieldIsRejected(String field) throws Exception {
        ObjectNode body = validBody("P101"); body.remove(field);
        mvc.perform(auth(post(collection())).content(body.toString())).andExpect(status().isBadRequest());
        assertRoomCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{"})
    void malformedOrEmptyRequestsAreRejected(String body) throws Exception {
        String room = createRoom("P101");
        mvc.perform(auth(post(collection())).content(body)).andExpect(status().isBadRequest());
        mvc.perform(auth(patch("/api/rooms/" + room)).content(body)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "page=2147483647", "size=0", "size=101", "size=abc",
            "sort=status,asc", "sort=area,bad", "sort=area", "sort=area,asc,id", "status=INVALID",
            "houseId=invalid", "floorId=invalid", "minPrice=-1", "maxPrice=-1", "minPrice=abc"})
    void invalidQueryReturns400(String query) throws Exception {
        String[] pair = query.split("=", 2);
        mvc.perform(auth(get("/api/rooms")).param(pair[0], pair[1])).andExpect(status().isBadRequest());
    }

    @Test
    void invalidPriceRangeAndHttpMethodAreRejected() throws Exception {
        mvc.perform(auth(get("/api/rooms")).param("minPrice", "100").param("maxPrice", "99"))
                .andExpect(status().isBadRequest());
        String room = createRoom("P101");
        mvc.perform(auth(put("/api/rooms/" + room)).content("{}"))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"));
    }

    @Test
    void acceptsExactFieldBoundaries() throws Exception {
        ObjectNode body = validBody("A".repeat(50)).put("area", new java.math.BigDecimal("99999999.99"))
                .put("basePrice", new java.math.BigDecimal("9999999999999.99"))
                .put("maxTenants", Integer.MAX_VALUE).put("amenitiesDescription", "A".repeat(5000));
        mvc.perform(auth(post(collection())).content(body.toString())).andExpect(status().isCreated());
        mvc.perform(auth(post(collection())).content(validBody("MIN").put("area", 0.01).put("basePrice", 0.01)
                .put("maxTenants", 1).toString())).andExpect(status().isCreated());
    }

    @Test
    void realRoomBlocksFloorAndHouseDeletion() throws Exception {
        createRoom("P101");
        mvc.perform(auth(delete("/api/floors/" + floorId))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2008));
        mvc.perform(auth(delete("/api/houses/" + houseId))).andExpect(status().isConflict());
        assertRoomCount(1);
        assertThat(houses.get(owner, houseId).totalFloors()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OCCUPIED", "RESERVED"})
    void inUseRoomsCannotBeDeletedOrHaveCapacityReducedWithoutRental(String statusValue) throws Exception {
        String room = createRoom("P101"); setStatus(room, statusValue);
        mvc.perform(auth(delete("/api/rooms/" + room))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2009));
        mvc.perform(auth(patch("/api/rooms/" + room)).content("{\"maxTenants\":1,\"area\":99}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(2013));
        mvc.perform(auth(get("/api/rooms/" + room))).andExpect(jsonPath("$.data.maxTenants").value(2))
                .andExpect(jsonPath("$.data.area").value(25.5));
        mvc.perform(auth(patch("/api/rooms/" + room)).content("{\"maxTenants\":3,\"basePrice\":4000000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(statusValue));
        assertRoomCount(1);
    }

    @Test
    void maintenanceRoomCanBeDeletedAndLocalDependenciesBlockDeletion() throws Exception {
        String room = createRoom("P101"); setStatus(room, "MAINTENANCE");
        UUID dependent = UUID.randomUUID();
        jdbc.update("INSERT INTO test_room_dependents VALUES (?, ?)", dependent, UUID.fromString(room));
        mvc.perform(auth(delete("/api/rooms/" + room))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2010));
        assertRoomCount(1);
        jdbc.update("DELETE FROM test_room_dependents WHERE id = ?", dependent);
        mvc.perform(auth(delete("/api/rooms/" + room))).andExpect(status().isNoContent());
        assertRoomCount(0);
    }

    @Test
    void databaseRejectsDuplicateOrOrphanRoomsAndFloorDeletion() throws Exception {
        String room = createRoom("P101"); createRoom("P102");
        assertThatThrownBy(() -> jdbc.update("UPDATE rooms SET room_number = 'P101' WHERE room_number = 'P102'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE rooms SET floor_id = ? WHERE id = ?", UUID.randomUUID(), UUID.fromString(room)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM floors WHERE id = ?", floorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentDuplicateCreationOnlyAllowsOneRoom() throws Exception {
        assertThat(race(() -> createStatus("p101"), () -> createStatus(" P101 "))).containsExactlyInAnyOrder(201, 409);
        assertRoomCount(1);
    }

    @Test
    void concurrentRenameToSameNumberOnlyAllowsOneChange() throws Exception {
        String a = createRoom("P101"); String b = createRoom("P102");
        assertThat(race(() -> patchStatus(a, "{\"roomNumber\":\"P103\"}"),
                () -> patchStatus(b, "{\"roomNumber\":\"P103\"}"))).containsExactlyInAnyOrder(200, 409);
        assertRoomCount(2);
    }

    @Test
    void concurrentIndependentPatchesDoNotLoseUpdates() throws Exception {
        String room = createRoom("P101");
        assertThat(race(() -> patchStatus(room, "{\"area\":30}"),
                () -> patchStatus(room, "{\"hasBalcony\":true}"))).containsExactly(200, 200);
        mvc.perform(auth(get("/api/rooms/" + room))).andExpect(jsonPath("$.data.area").value(30))
                .andExpect(jsonPath("$.data.hasBalcony").value(true));
    }

    @Test
    void concurrentDeletionOnlySucceedsOnce() throws Exception {
        String room = createRoom("P101");
        Callable<Integer> action = () -> mvc.perform(auth(delete("/api/rooms/" + room))).andReturn().getResponse().getStatus();
        assertThat(race(action, action)).containsExactlyInAnyOrder(204, 404);
        assertRoomCount(0);
    }

    @Test
    void roomCreationRacingFloorDeletionCannotCreateOrphan() throws Exception {
        var result = race(() -> createStatus("P101"),
                () -> mvc.perform(auth(delete("/api/floors/" + floorId))).andReturn().getResponse().getStatus());
        assertThat((Object) result).isIn(List.of(201, 409), List.of(404, 204));
        assertRoomCount(result.getFirst() == 201 ? 1 : 0);
        assertThat(houses.get(owner, houseId).totalFloors()).isEqualTo(result.getFirst() == 201 ? 1 : 0);
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var b = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            boolean bothReady = ready.await(5, TimeUnit.SECONDS); start.countDown();
            assertThat(bothReady).isTrue();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private int createStatus(String number) throws Exception {
        return mvc.perform(auth(post(collection())).content(validBody(number).toString())).andReturn().getResponse().getStatus();
    }

    private int patchStatus(String room, String body) throws Exception {
        return mvc.perform(auth(patch("/api/rooms/" + room)).content(body)).andReturn().getResponse().getStatus();
    }

    private MockHttpServletRequestBuilder operation(String operation, String floor, String room) {
        return switch (operation) {
            case "POST" -> post("/api/floors/" + floor + "/rooms").contentType(MediaType.APPLICATION_JSON).content(validBody("P102").toString());
            case "LIST" -> get("/api/rooms");
            case "GET" -> get("/api/rooms/" + room);
            case "PATCH" -> patch("/api/rooms/" + room).contentType(MediaType.APPLICATION_JSON).content("{\"area\":30}");
            case "DELETE" -> delete("/api/rooms/" + room);
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private ObjectNode validBody(String number) {
        return mapper.createObjectNode().put("roomNumber", number).put("area", 25.5).put("basePrice", 3500000).put("maxTenants", 2);
    }

    private String createRoom(String number) throws Exception {
        return id(mvc.perform(auth(post(collection())).content(validBody(number).toString()))
                .andExpect(status().isCreated()).andReturn());
    }

    private String id(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asString();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String collection() { return "/api/floors/" + floorId + "/rooms"; }

    private void assertRoomCount(int expected) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rooms", Integer.class)).isEqualTo(expected);
    }

    private void setStatus(String room, String value) {
        jdbc.update("UPDATE rooms SET status = ? WHERE id = ?", value, UUID.fromString(room));
    }

    private User user(Role role) {
        String unique = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().username(unique).email(unique + "@example.com")
                .passwordHash("test-only").fullName("Test").phoneNumber("0900000000")
                .role(role).status(UserStatus.ACTIVE).build());
    }

    private UUID house(UUID landlord) {
        var request = new HouseRequest(); request.setName("Nhà A"); request.setAddressStreet("12 A");
        request.setWard("Phường A"); request.setCity("TP C");
        return houses.create(landlord, request).id();
    }

    private UUID floor(UUID landlord, UUID house, int number) {
        var request = new FloorRequest(); request.setFloorNumber(number); request.setName("Tầng " + number);
        return floors.create(landlord, house, request).id();
    }
}
