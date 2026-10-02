package com.team4.core.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class LandlordRoomRequest {
    @NotNull(message = "houseId là bắt buộc")
    UUID houseId;

    @NotNull(message = "floorId là bắt buộc")
    UUID floorId;

    @NotBlank(message = "Số phòng là bắt buộc")
    @Size(max = 50, message = "Số phòng không được vượt quá 50 ký tự")
    String roomNumber;

    @DecimalMin(value = "0.01", message = "Diện tích phải lớn hơn 0")
    BigDecimal area;

    @DecimalMin(value = "0.00", message = "Giá phòng không được âm")
    BigDecimal basePrice;

    @Min(value = 1, message = "Số người tối đa phải từ 1")
    int maxTenants;

    boolean hasPrivateBathroom;
    boolean hasAirConditioner;
    boolean hasWaterHeater;
    boolean hasBalcony;

    @Size(max = 3000, message = "Mô tả tiện ích không được vượt quá 3000 ký tự")
    String amenitiesDescription;

    @NotBlank(message = "Tiêu đề bài đăng là bắt buộc")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    String title;

    @NotBlank(message = "Mô tả bài đăng là bắt buộc")
    String description;

    @DecimalMin(value = "0.00", message = "Tiền cọc không được âm")
    @NotNull(message = "Tiền cọc là bắt buộc")
    BigDecimal depositAmount;

    @NotNull(message = "Ngày có thể vào ở là bắt buộc")
    LocalDate availableFrom;
}
