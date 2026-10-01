package com.team4.core.merge;

import com.team4.core.entities.Post;
import com.team4.core.entities.User;
import com.team4.core.enums.PostStatus;
import com.team4.core.enums.Role;
import com.team4.core.enums.UserStatus;
import com.team4.core.repositories.PostRepository;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "debug=false", "spring.main.banner-mode=off",
        "spring.datasource.url=jdbc:h2:mem:merged;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
class MergedWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired JwtTokenProvider tokens;
    @Autowired PasswordEncoder passwords;
    @Autowired JsonMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        for (String table : new String[]{"posts", "notifications", "audit_logs", "rooms", "floors", "houses", "users"}) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    @Test
    void approvalPropertyManagementModerationAndAccountLockWorkTogether() throws Exception {
        String username = "landlord_" + UUID.randomUUID().toString().substring(0, 8);
        var registration = mapper.createObjectNode().put("username", username).put("password", "Password123!")
                .put("email", username + "@example.com").put("fullName", "Chủ trọ")
                .put("phoneNumber", "0900000000").put("role", "LANDLORD");
        MvcResult registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registration.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist()).andReturn();
        String landlordId = mapper.readTree(registered.getResponse().getContentAsString()).at("/data/user/id").asString();
        String loginBody = mapper.createObjectNode().put("username", username).put("password", "Password123!").toString();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isUnauthorized());
        User pending = users.findById(UUID.fromString(landlordId)).orElseThrow();
        mvc.perform(auth(get("/api/houses"), tokens.generateToken(pending))).andExpect(status().isUnauthorized());

        User admin = users.saveAndFlush(User.builder().username("admin").email("admin@example.com")
                .passwordHash(passwords.encode("Password123!")).fullName("Admin").phoneNumber("0900000001")
                .role(Role.ADMIN).status(UserStatus.ACTIVE).build());
        String adminToken = tokens.generateToken(admin);
        mvc.perform(auth(post("/api/admin/users/" + landlordId + "/approve-landlord"), adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk()).andReturn();
        String landlordToken = mapper.readTree(login.getResponse().getContentAsString()).at("/data/accessToken").asString();
        String house = createdId(mvc.perform(auth(post("/api/houses"), landlordToken).content("""
                {"name":"Nhà A","addressStreet":"12 A","ward":"Phường A","city":"TP C"}
                """)).andExpect(status().isCreated()).andReturn());
        String floor = createdId(mvc.perform(auth(post("/api/houses/" + house + "/floors"), landlordToken)
                .content("{\"floorNumber\":0,\"name\":\"Trệt\"}"))
                .andExpect(status().isCreated()).andReturn());
        String room = createdId(mvc.perform(auth(post("/api/floors/" + floor + "/rooms"), landlordToken)
                .content("{\"roomNumber\":\"101\",\"area\":25,\"basePrice\":3000000,\"maxTenants\":2}"))
                .andExpect(status().isCreated()).andReturn());
        mvc.perform(auth(get("/api/admin/users/" + landlordId + "/properties"), adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].rooms[0].id").value(room))
                .andExpect(jsonPath("$.data[0].totalFloors").value(1))
                .andExpect(jsonPath("$.data[0].address").value("12 A, Phường A, TP C"));
        mvc.perform(auth(get("/api/admin/users").param("role", "LANDLORD"), adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(landlordId));
        mvc.perform(auth(get("/api/houses"), landlordToken)).andExpect(jsonPath("$.data.items[0].id").value(house));

        Post post = posts.saveAndFlush(Post.builder().landlordId(UUID.fromString(landlordId)).roomId(UUID.fromString(room))
                .title("Phòng 101").description("Cho thuê").rentalPrice(new BigDecimal("3000000"))
                .depositAmount(new BigDecimal("3000000")).availableFrom(LocalDate.of(2026, 10, 1))
                .status(PostStatus.PENDING).build());
        mvc.perform(auth(get("/api/admin/posts"), adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(post.getId().toString()));
        mvc.perform(auth(post("/api/admin/posts/" + post.getId() + "/approve"), adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        mvc.perform(auth(delete("/api/rooms/" + room), landlordToken)).andExpect(status().isConflict());
        mvc.perform(auth(post("/api/admin/users/" + landlordId + "/lock"), adminToken)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/houses"), landlordToken)).andExpect(status().isUnauthorized());
        mvc.perform(auth(post("/api/floors/" + floor + "/rooms"), landlordToken).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(auth(post("/api/admin/users/" + landlordId + "/unlock"), adminToken)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/rooms/" + room), landlordToken)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications", Integer.class)).isEqualTo(4);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private String createdId(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asString();
    }
}
