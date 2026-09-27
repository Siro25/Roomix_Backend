package com.team4.core.services.impl;

import com.team4.core.services.UserService;
import com.team4.core.dtos.response.UserProfileResponse;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.repositories.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID id) {
        return userRepository.findById(id)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
