package com.team4.rental.dtos.response;

import java.time.Instant;
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
public class CoreLandlordContactResponse {
    UUID id;
    String fullName;
    String phoneNumber;
    String zalo;
    String avatarUrl;
    Instant joinedAt;
}
