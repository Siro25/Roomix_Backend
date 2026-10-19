package com.team4.core.services;

import com.team4.core.dtos.response.UserProfileResponse;
import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfile(UUID id);
}
