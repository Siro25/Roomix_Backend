package com.team4.core.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class PostRejectionRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
    String reason;
}
