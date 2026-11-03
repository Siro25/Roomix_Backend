package com.team4.core.controllers;

import com.team4.core.dtos.request.RoomSearchRequest;
import com.team4.core.dtos.response.ApiResponse;
import com.team4.core.dtos.response.PageResponse;
import com.team4.core.dtos.response.RoomDetailResponse;
import com.team4.core.dtos.response.RoomSummaryResponse;
import com.team4.core.services.RoomQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomQueryService roomQueryService;

    @GetMapping("/search")
    public ApiResponse<PageResponse<RoomSummaryResponse>> search(
            @Valid @ModelAttribute RoomSearchRequest request) {
        return ApiResponse.<PageResponse<RoomSummaryResponse>>builder()
                .message("Tìm kiếm phòng thành công")
                .data(roomQueryService.search(request))
                .build();
    }

    @GetMapping("/{roomId}")
    public ApiResponse<RoomDetailResponse> getDetail(@PathVariable UUID roomId) {
        return ApiResponse.<RoomDetailResponse>builder()
                .message("Lấy chi tiết phòng thành công")
                .data(roomQueryService.getDetail(roomId))
                .build();
    }
}
