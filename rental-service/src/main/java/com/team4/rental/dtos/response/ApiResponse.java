package com.team4.rental.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000;
    private String message;
    private T data;
    private Map<String, String> errors;
}
