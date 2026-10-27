package com.team4.core.services.impl;

import com.team4.core.dtos.request.UpdateProfileRequest;
import com.team4.core.dtos.response.LandlordContactResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.entities.User;
import com.team4.core.enums.Role;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.UserRepository;
import com.team4.core.services.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID id) {
        return userRepository.findById(id)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.updateProfile(
                request.getFullName().strip(),
                request.getPhoneNumber().strip(),
                request.getAvatarUrl() == null || request.getAvatarUrl().isBlank()
                        ? null
                        : request.getAvatarUrl().strip(),
                request.getZalo() == null || request.getZalo().isBlank()
                        ? null
                        : request.getZalo().strip());
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public LandlordContactResponse getLandlordContact(UUID landlordId) {
        User landlord = userRepository.findByIdAndRole(landlordId, Role.LANDLORD)
                .orElseThrow(() -> new AppException(ErrorCode.LANDLORD_PROFILE_NOT_FOUND));
        return LandlordContactResponse.builder()
                .id(landlord.getId())
                .fullName(landlord.getFullName())
                .phoneNumber(landlord.getPhoneNumber())
                .zalo(landlord.getZalo())
                .avatarUrl(landlord.getAvatarUrl())
                .joinedAt(landlord.getCreatedAt())
                .build();
    }
}
