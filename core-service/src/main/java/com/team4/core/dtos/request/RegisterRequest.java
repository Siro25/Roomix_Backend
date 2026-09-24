package com.team4.core.dtos.request;

import com.team4.core.enums.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,50}$")
    String username;

    @NotBlank
    @Size(max = 100)
    String fullName;

    @NotBlank
    @Email
    @Size(max = 254)
    String email;

    @NotBlank
    @Size(min = 8, max = 72)
    String password;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{9,15}$")
    String phoneNumber;

    @Builder.Default
    Role role = Role.TENANT;
}
