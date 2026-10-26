package com.team4.core.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "Mã đặt lại mật khẩu là bắt buộc")
    String token;

    @NotBlank(message = "Mật khẩu mới là bắt buộc")
    @Size(min = 8, max = 72, message = "Mật khẩu mới phải từ 8 đến 72 ký tự")
    String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    String confirmPassword;
}
