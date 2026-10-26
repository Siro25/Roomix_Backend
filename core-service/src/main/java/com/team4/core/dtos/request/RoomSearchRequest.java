package com.team4.core.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSearchRequest {
    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal minPrice;

    @DecimalMin(value = "0.0", inclusive = true)
    BigDecimal maxPrice;

    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal minArea;

    String location;

    @Builder.Default
    List<String> amenities = new ArrayList<>();

    @Builder.Default
    String sort = "newest";

    @Builder.Default
    @Min(0)
    int page = 0;

    @Builder.Default
    @Min(1)
    @Max(100)
    int size = 20;
}
