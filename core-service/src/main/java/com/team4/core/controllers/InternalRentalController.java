package com.team4.core.controllers;

import com.team4.core.dtos.response.*;
import com.team4.core.exception.*;
import com.team4.core.repositories.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rental")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT', 'LANDLORD')")
public class InternalRentalController {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @GetMapping("/rooms/{roomId}")
    @Transactional(readOnly = true)
    public ApiResponse<CoreRoomResponse> room(@PathVariable UUID roomId) {
        var post = postRepository.findFirstByRoomIdOrderByCreatedAtDesc(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        var room = post.getRoom();
        return ApiResponse.<CoreRoomResponse>builder().data(CoreRoomResponse.builder()
                .roomId(room.getId()).postId(post.getId())
                .landlordId(room.getFloor().getHouse().getLandlordId())
                .postStatus(post.getStatus().name()).roomStatus(room.getStatus().name())
                .rented(room.isRented()).availableFrom(post.getAvailableFrom()).build()).build();
    }

    @GetMapping("/account")
    public ApiResponse<CoreAccountResponse> account(@AuthenticationPrincipal Jwt jwt) {
        var user = userRepository.findById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return ApiResponse.<CoreAccountResponse>builder().data(CoreAccountResponse.builder()
                .id(user.getId()).role(user.getRole().name()).status(user.getStatus().name()).build()).build();
    }
}
