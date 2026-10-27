package com.team4.core.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên là bắt buộc")
    @Size(max = 100)
    String fullName;
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^[0-9+() .-]{7,20}$", message = "Số điện thoại không hợp lệ")
    String phoneNumber;
    @Size(max = 500)
    String avatarUrl;

    @Size(max = 100, message = "Thông tin Zalo không được vượt quá 100 ký tự")
    String zalo;
}
