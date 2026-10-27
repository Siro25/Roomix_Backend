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
public class ContactCardResponse {
    UUID id;
    UUID rentalRequestId;
    UUID landlordId;
    String landlordName;
    String landlordPhone;
    String landlordZalo;
    String landlordAvatarUrl;
    String note;
    Instant issuedAt;
}
