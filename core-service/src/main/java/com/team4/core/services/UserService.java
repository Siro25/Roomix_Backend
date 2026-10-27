package com.team4.core.services;

import com.team4.core.dtos.request.UpdateProfileRequest;
import com.team4.core.dtos.response.LandlordContactResponse;
import com.team4.core.dtos.response.UserProfileResponse;
import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfile(UUID id);

    UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request);

    LandlordContactResponse getLandlordContact(UUID landlordId);
}
