package com.team4.rental.dtos.response;

import com.team4.rental.entities.Tenancy;
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
public class RentalAcceptanceResponse {
    Tenancy tenancy;
    ContactCardResponse contactCard;
}
