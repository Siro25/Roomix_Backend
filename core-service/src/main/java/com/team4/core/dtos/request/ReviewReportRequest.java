package com.team4.core.dtos.request;

import com.team4.core.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
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
public class ReviewReportRequest {
    @NotNull(message = "Trạng thái là bắt buộc")
    ReportStatus status;
    @Size(max = 3000)
    String resolution;
}
