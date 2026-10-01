package com.team4.core.controllers;

import com.team4.core.config.SecurityConfig;
import com.team4.core.dtos.response.HouseResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.repositories.UserRepository;
import com.team4.core.security.JwtTokenProvider;
import com.team4.core.services.HouseService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.team4.core.entities.User;
import com.team4.core.enums.UserStatus;
import com.team4.core.security.AccountStatusFilter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HouseController.class, properties = "debug=false")
@Import({SecurityConfig.class, AccountStatusFilter.class})
class HouseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean HouseService service;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean UserRepository users;
    private final UUID owner = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void activeUserExists() {
        when(users.findById(owner)).thenReturn(Optional.of(User.builder().id(owner).status(UserStatus.ACTIVE).build()));
    }

    @Test
    void requiresAuthenticationAndLandlordRole() throws Exception {
        mvc.perform(get("/api/houses")).andExpect(status().isUnauthorized());
        for (String role : List.of("TENANT", "ADMIN")) {
            mvc.perform(get("/api/houses").with(identity(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void createsWithLocationAndResponseEnvelope() throws Exception {
        when(service.create(eq(owner), any())).thenReturn(response());
        mvc.perform(post("/api/houses").with(identity("LANDLORD"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nhà A\",\"addressStreet\":\"12 A\",\"ward\":\"A\",\"city\":\"B\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/houses/" + id))
                .andExpect(jsonPath("$.code").value(1000)).andExpect(jsonPath("$.data.landlordId").value(owner.toString()));
    }

    @Test
    void listsReadsUpdatesAndDeletes() throws Exception {
        when(service.list(owner, null, null, null, 0, 20, "createdAt,desc"))
                .thenReturn(new PageResponse<>(List.of(response()), 0, 20, 1, 1));
        when(service.get(owner, id)).thenReturn(response());
        when(service.update(eq(owner), eq(id), any())).thenReturn(response());
        mvc.perform(get("/api/houses").with(identity("LANDLORD")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/houses/" + id).with(identity("LANDLORD")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id.toString()));
        mvc.perform(patch("/api/houses/" + id).with(identity("LANDLORD"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"description\":null}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/houses/" + id).with(identity("LANDLORD")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(owner, id);
    }

    @Test
    void invalidInputReturns400InsteadOf500() throws Exception {
        mvc.perform(get("/api/houses/not-a-uuid").with(identity("LANDLORD")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
        mvc.perform(get("/api/houses?page=abc").with(identity("LANDLORD")))
                .andExpect(status().isBadRequest());
        for (String body : List.of("{}", "{\"name\":\"\"}", "{\"landlordId\":\"other\"}")) {
            mvc.perform(post("/api/houses").with(identity("LANDLORD"))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    private RequestPostProcessor identity(String role) {
        return jwt().jwt(token -> token.subject(owner.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private HouseResponse response() {
        return new HouseResponse(id, owner, "Nhà A", "12 A", "A", null, "B",
                null, null, null, 0, null, null);
    }
}
