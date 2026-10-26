package com.team4.core.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HouseRequest {
    @NotBlank(message = "Tên nhà là bắt buộc")
    @Size(max = 150, message = "Tên nhà không được vượt quá 150 ký tự")
    String name;

    @NotBlank(message = "Địa chỉ là bắt buộc")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    String addressStreet;

    @NotBlank(message = "Phường/xã là bắt buộc")
    @Size(max = 100, message = "Phường/xã không được vượt quá 100 ký tự")
    String ward;

    @NotBlank(message = "Quận/huyện là bắt buộc")
    @Size(max = 100, message = "Quận/huyện không được vượt quá 100 ký tự")
    String district;

    @NotBlank(message = "Tỉnh/thành phố là bắt buộc")
    @Size(max = 100, message = "Tỉnh/thành phố không được vượt quá 100 ký tự")
    String city;

    @DecimalMin(value = "-90.0000000", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0000000", message = "Vĩ độ không hợp lệ")
    BigDecimal latitude;

    @DecimalMin(value = "-180.0000000", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0000000", message = "Kinh độ không hợp lệ")
    BigDecimal longitude;

    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    String description;

    @NotNull(message = "Tổng số tầng là bắt buộc")
    @Min(value = 1, message = "Tổng số tầng phải từ 1")
    Integer totalFloors;
}
