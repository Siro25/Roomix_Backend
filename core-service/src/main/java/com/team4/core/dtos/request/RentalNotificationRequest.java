package com.team4.core.dtos.request;

import com.team4.core.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RentalNotificationRequest {
    @NotNull
    UUID userId;

    @NotNull
    UUID referenceId;

    @NotNull
    NotificationType type;

    @NotBlank
    @Size(max = 80)
    String event;

    @NotBlank
    @Size(max = 150)
    String title;

    @NotBlank
    @Size(max = 2000)
    String content;

    @Pattern(regexp = "^/.*", message = "targetUrl phải là đường dẫn nội bộ")
    @Size(max = 500)
    String targetUrl;
}
