package com.team4.core.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorRequest {
    @Min(value = 1, message = "Số tầng phải từ 1")
    int floorNumber;

    @Size(max = 500, message = "Mô tả tầng không được vượt quá 500 ký tự")
    String description;
}
