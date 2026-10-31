package com.team4.rental.events;

import com.team4.rental.dtos.request.CoreNotificationRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RentalNotificationEvent {
    CoreNotificationRequest notification;
    String accessToken;
}
