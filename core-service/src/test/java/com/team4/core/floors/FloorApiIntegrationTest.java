package com.team4.core.floors;

import com.team4.core.dtos.request.HouseRequest;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.JwtTokenProvider;
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT, MVC, service, repositories and Flyway; isolated H2 PostgreSQL-mode database. */
@SpringBootTest(properties = {
        "debug=false", "spring.main.banner-mode=off",
        "spring.datasource.url=jdbc:h2:mem:floors;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
class FloorApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired HouseService houses;
    @Autowired JwtTokenProvider tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    private UUID owner;
    private UUID houseId;
    private String token;

    @BeforeEach
    void setUp() {
        User landlord = user(Role.LANDLORD);
        owner = landlord.getId();
        token = tokens.generateToken(landlord);
        houseId = house(owner);
        // Test-only child table models the future ROOM.floor_id RESTRICT contract.
        jdbc.execute("CREATE TABLE IF NOT EXISTS test_room_refs (id UUID PRIMARY KEY, "
                + "floor_id UUID NOT NULL REFERENCES floors(id) ON DELETE RESTRICT)");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM test_room_refs");
        jdbc.update("DELETE FROM floors");
        jdbc.update("DELETE FROM houses");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void completeCrudPreservesPatchSemanticsAndFloorCount() throws Exception {
        MvcResult created = mvc.perform(auth(post(collection())).content("""
                {"floorNumber":0,"name":"  Tầng trệt  ","description":" Ghi chú "}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.name").value("Tầng trệt"))
                .andExpect(jsonPath("$.data.houseId").value(houseId.toString())).andReturn();
        String id = id(created);
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/floors/" + id);
        assertCount(1);
        mvc.perform(auth(get("/api/floors/" + id))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.floorNumber").value(0));
        mvc.perform(auth(patch("/api/floors/" + id)).content("{\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Tầng trệt"))
                .andExpect(jsonPath("$.data.description").doesNotExist());
        mvc.perform(auth(patch("/api/floors/" + id)).content("{\"floorNumber\":5,\"name\":\" Tầng 5 \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.floorNumber").value(5))
                .andExpect(jsonPath("$.data.name").value("Tầng 5"));
        assertCount(1); // Count rows, not highest floor number.
        mvc.perform(auth(delete("/api/floors/" + id))).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertCount(0);
        mvc.perform(auth(get("/api/floors/" + id))).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/houses/" + houseId))).andExpect(status().isNoContent());
    }

    @Test
    void listIsPaginatedSortedAndScopedToHouse() throws Exception {
        createFloor(3, "C"); createFloor(0, "A"); createFloor(1, "B");
        UUID secondHouse = house(owner);
        mvc.perform(auth(post("/api/houses/" + secondHouse + "/floors"))
                .content(validBody(0, "Other"))).andExpect(status().isCreated());
        mvc.perform(auth(get(collection())).param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].floorNumber").value(0))
                .andExpect(jsonPath("$.data.items[1].floorNumber").value(1));
        mvc.perform(auth(get(collection())).param("sort", "name,desc").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].name").value("A"));
        mvc.perform(auth(get(collection())).param("page", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void emptyHouseReturnsEmptyPage() throws Exception {
        mvc.perform(auth(get(collection()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void duplicateCreateAndRenameRollbackWithoutChangingCount() throws Exception {
        String first = createFloor(0, "A");
        String second = createFloor(1, "B");
        mvc.perform(auth(post(collection())).content(validBody(0, "Duplicate")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2005));
        mvc.perform(auth(patch("/api/floors/" + second)).content("{\"floorNumber\":0,\"name\":\"Changed\"}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(get("/api/floors/" + second))).andExpect(jsonPath("$.data.name").value("B"))
                .andExpect(jsonPath("$.data.floorNumber").value(1));
        mvc.perform(auth(patch("/api/floors/" + first)).content("{\"floorNumber\":0}"))
                .andExpect(status().isOk());
        assertCount(2);
    }

    @Test
    void deleteWithChildReferenceRollsBackAndHouseCannotBeDeletedWithFloors() throws Exception {
        String floor = createFloor(0, "A");
        jdbc.update("INSERT INTO test_room_refs VALUES (?, ?)", UUID.randomUUID(), UUID.fromString(floor));
        mvc.perform(auth(delete("/api/floors/" + floor)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2008));
        assertCount(1);
        mvc.perform(auth(get("/api/floors/" + floor))).andExpect(status().isOk());
        mvc.perform(auth(delete("/api/houses/" + houseId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2007));
        // Even a stale counter cannot bypass the database FK.
        jdbc.update("UPDATE houses SET total_floors = 0 WHERE id = ?", houseId);
        mvc.perform(auth(delete("/api/houses/" + houseId))).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "LIST", "GET", "PATCH", "DELETE"})
    void everyEndpointRequiresValidAuthenticationAndLandlordRole(String operation) throws Exception {
        String floor = createFloor(0, "A");
        mvc.perform(operation(operation, houseId.toString(), floor)).andExpect(status().isUnauthorized());
        mvc.perform(operation(operation, houseId.toString(), floor).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.TENANT, Role.ADMIN)) {
            mvc.perform(operation(operation, houseId.toString(), floor)
                    .header("Authorization", "Bearer " + tokens.generateToken(user(role))))
                    .andExpect(status().isForbidden());
        }
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "LIST", "GET", "PATCH", "DELETE"})
    void everyEndpointHidesOtherOwnersAndMissingResources(String operation) throws Exception {
        String floor = createFloor(0, "A");
        String anotherToken = tokens.generateToken(user(Role.LANDLORD));
        mvc.perform(operation(operation, houseId.toString(), floor).header("Authorization", "Bearer " + anotherToken))
                .andExpect(status().isNotFound());
        mvc.perform(auth(operation(operation, UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "LIST", "GET", "PATCH", "DELETE"})
    void everyEndpointRejectsMalformedPathId(String operation) throws Exception {
        mvc.perform(auth(operation(operation, "not-uuid", "not-uuid")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
    }

    static Stream<String> invalidBodies() {
        return Stream.of("{\"floorNumber\":1.5,\"name\":\"A\"}",
                "{\"floorNumber\":\"1\",\"name\":\"A\"}",
                "{\"floorNumber\":true,\"name\":\"A\"}",
                "{}", "null", "[]", "{", "{\"floorNumber\":0}",
                "{\"name\":\"A\"}", "{\"floorNumber\":null,\"name\":\"A\"}",
                "{\"floorNumber\":-1,\"name\":\"A\"}", "{\"floorNumber\":2147483648,\"name\":\"A\"}",
                "{\"floorNumber\":0,\"name\":null}", "{\"floorNumber\":0,\"name\":\"  \"}",
                validBody(0, "A".repeat(101)),
                "{\"floorNumber\":0,\"name\":\"A\",\"description\":\"" + "x".repeat(256) + "\"}",
                "{\"floorNumber\":0,\"name\":\"A\",\"houseId\":\"other\"}",
                "{\"floorNumber\":0,\"name\":\"A\",\"id\":\"other\"}");
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidCreateDoesNotWriteRowsOrCount(String body) throws Exception {
        mvc.perform(auth(post(collection())).content(body)).andExpect(status().isBadRequest());
        assertCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{\"floorNumber\":null}", "{\"floorNumber\":-1}",
            "{\"name\":null}", "{\"name\":\"  \"}", "{\"houseId\":\"other\"}",
            "{\"unknown\":1}", "{\"floorNumber\":\"invalid\"}"})
    void invalidPatchPreservesExistingData(String body) throws Exception {
        String floor = createFloor(0, "A");
        mvc.perform(auth(patch("/api/floors/" + floor)).content(body)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/floors/" + floor))).andExpect(jsonPath("$.data.name").value("A"));
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "size=0", "size=101", "size=abc",
            "sort=id,asc", "sort=name,wrong", "sort=name", "sort=name,asc,id"})
    void invalidListParametersReturn400(String query) throws Exception {
        String[] parts = query.split("=", 2);
        mvc.perform(auth(get(collection())).param(parts[0], parts[1])).andExpect(status().isBadRequest());
    }

    @Test
    void acceptsMaximumLengthsAndRejectsOversizedPatch() throws Exception {
        var body = mapper.createObjectNode().put("floorNumber", Integer.MAX_VALUE)
                .put("name", "A".repeat(100)).put("description", "B".repeat(255));
        String floor = id(mvc.perform(auth(post(collection())).content(body.toString()))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(auth(patch("/api/floors/" + floor))
                .content(mapper.createObjectNode().put("description", "B".repeat(256)).toString()))
                .andExpect(status().isBadRequest());
        assertCount(1);
    }

    @Test
    void databaseEnforcesUniqueNumberAndForeignKeys() throws Exception {
        createFloor(0, "A");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO floors VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), houseId, 0, "Duplicate", null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO floors VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), 1, "Orphan", null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM houses WHERE id = ?", houseId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertCount(1);
    }

    @Test
    void concurrentDifferentCreatesKeepAccurateCount() throws Exception {
        assertThat(race(() -> createStatus(0), () -> createStatus(1))).containsExactlyInAnyOrder(201, 201);
        assertCount(2);
    }

    @Test
    void concurrentDuplicateCreatesOnlyOneSucceeds() throws Exception {
        assertThat(race(() -> createStatus(0), () -> createStatus(0))).containsExactlyInAnyOrder(201, 409);
        assertCount(1);
    }

    @Test
    void concurrentRenamesToSameNumberOnlyOneSucceeds() throws Exception {
        String a = createFloor(0, "A"); String b = createFloor(1, "B");
        assertThat(race(() -> renameStatus(a, 2), () -> renameStatus(b, 2)))
                .containsExactlyInAnyOrder(200, 409);
        assertCount(2);
    }

    @Test
    void concurrentDeleteOfSameFloorOnlyDecrementsOnce() throws Exception {
        String floor = createFloor(0, "A");
        Callable<Integer> delete = () -> mvc.perform(auth(delete("/api/floors/" + floor)))
                .andReturn().getResponse().getStatus();
        assertThat(race(delete, delete)).containsExactlyInAnyOrder(204, 404);
        assertCount(0);
    }

    @Test
    void floorCreationRacingHouseDeletionNeverLeavesAnOrphan() throws Exception {
        var statuses = race(() -> createStatus(0), () -> mvc.perform(auth(delete("/api/houses/" + houseId)))
                .andReturn().getResponse().getStatus());
        assertThat((Object) statuses).isIn(List.of(201, 409), List.of(404, 204));
        if (statuses.getFirst() == 201) assertCount(1);
        else assertThat(jdbc.queryForObject("SELECT count(*) FROM floors WHERE house_id = ?", Integer.class, houseId)).isZero();
    }

    @Test
    void concurrentPatchesPreserveBothIndependentChanges() throws Exception {
        String floor = createFloor(0, "A");
        assertThat(race(
                () -> mvc.perform(auth(patch("/api/floors/" + floor)).content("{\"name\":\"B\"}"))
                        .andReturn().getResponse().getStatus(),
                () -> mvc.perform(auth(patch("/api/floors/" + floor)).content("{\"description\":\"Note\"}"))
                        .andReturn().getResponse().getStatus())).containsExactly(200, 200);
        mvc.perform(auth(get("/api/floors/" + floor))).andExpect(jsonPath("$.data.name").value("B"))
                .andExpect(jsonPath("$.data.description").value("Note"));
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"houses", "floors"})
    void oversizedOffsetsReturn400AndValidBoundaryStillWorks(String resource) throws Exception {
        String path = resource.equals("houses") ? "/api/houses" : collection();
        mvc.perform(auth(get(path)).param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
        mvc.perform(auth(get(path)).param("page", "21474837").param("size", "100"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(get(path)).param("page", "2147483647").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"houses", "floors"})
    void unsupportedMethodPreserves405AndAllowHeader(String resource) throws Exception {
        String id = resource.equals("houses") ? houseId.toString() : createFloor(0, "A");
        mvc.perform(auth(put("/api/" + resource + "/" + id)).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("PATCH")))
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void frameworkErrorsPreserve415And404() throws Exception {
        mvc.perform(auth(post(collection())).contentType(MediaType.TEXT_PLAIN).content("invalid"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(auth(get("/api/floors/not/a/route"))).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "true", "{}", "[]"})
    void textFieldsRejectWrongJsonTypesOnCreateAndPatch(String value) throws Exception {
        String floor = createFloor(0, "Original");
        for (String field : List.of("name", "description")) {
            String body = field.equals("name")
                    ? "{\"floorNumber\":1,\"name\":" + value + "}"
                    : "{\"floorNumber\":1,\"name\":\"B\",\"description\":" + value + "}";
            mvc.perform(auth(post(collection())).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
            mvc.perform(auth(patch("/api/floors/" + floor)).content("{\"" + field + "\":" + value + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(auth(get("/api/floors/" + floor))).andExpect(jsonPath("$.data.name").value("Original"));
        assertCount(1);
    }

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var b = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(bothReady).isTrue();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private int createStatus(int number) throws Exception {
        return mvc.perform(auth(post(collection())).content(validBody(number, "Tầng")))
                .andReturn().getResponse().getStatus();
    }

    private int renameStatus(String id, int number) throws Exception {
        return mvc.perform(auth(patch("/api/floors/" + id)).content("{\"floorNumber\":" + number + "}"))
                .andReturn().getResponse().getStatus();
    }

    private MockHttpServletRequestBuilder operation(String operation, String house, String floor) {
        String path = "/api/floors/" + floor;
        return switch (operation) {
            case "POST" -> post("/api/houses/" + house + "/floors").contentType(MediaType.APPLICATION_JSON).content(validBody(1, "B"));
            case "LIST" -> get("/api/houses/" + house + "/floors");
            case "GET" -> get(path);
            case "PATCH" -> patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"B\"}");
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private String createFloor(int number, String name) throws Exception {
        return id(mvc.perform(auth(post(collection())).content(validBody(number, name)))
                .andExpect(status().isCreated()).andReturn());
    }

    private String id(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asString();
    }

    private static String validBody(int number, String name) {
        return "{\"floorNumber\":" + number + ",\"name\":\"" + name + "\"}";
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String collection() { return "/api/houses/" + houseId + "/floors"; }

    private void assertCount(int expected) {
        assertThat(jdbc.queryForObject("SELECT total_floors FROM houses WHERE id = ?", Integer.class, houseId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM floors WHERE house_id = ?", Integer.class, houseId)).isEqualTo(expected);
    }

    private User user(Role role) {
        String unique = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().username(unique).email(unique + "@example.com")
                .passwordHash("test-only").fullName("Test").phoneNumber("0900000000")
                .role(role).status(UserStatus.ACTIVE).build());
    }

    private UUID house(UUID landlord) {
        var request = new HouseRequest();
        request.setName("Nhà A"); request.setAddressStreet("12 A");
        request.setWard("Phường A"); request.setCity("TP C");
        return houses.create(landlord, request).id();
    }
}
