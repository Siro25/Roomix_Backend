package com.team4.core.dtos.request;

import com.team4.core.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateReportRequest {
    @NotNull(message = "Loại đối tượng báo cáo là bắt buộc")
    ReportTargetType targetType;
    @NotNull(message = "ID đối tượng báo cáo là bắt buộc")
    UUID targetId;
    @NotBlank(message = "Lý do là bắt buộc")
    @Size(max = 200)
    String reason;
    @Size(max = 3000)
    String description;
}
