package com.team4.rental.external;

import com.team4.rental.dtos.request.ContactCardNotificationRequest;
import com.team4.rental.dtos.request.CoreNotificationRequest;
import com.team4.rental.dtos.response.ApiResponse;
import com.team4.rental.dtos.response.CoreAccountResponse;
import com.team4.rental.dtos.response.CoreLandlordContactResponse;
import com.team4.rental.dtos.response.CoreLandlordDashboardResponse;
import com.team4.rental.dtos.response.CoreRoomResponse;
import com.team4.rental.exceptions.AppException;
import com.team4.rental.exceptions.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class CoreServiceClient {
    private final RestClient coreRestClient;

    public CoreRoomResponse getRoom(UUID roomId, String accessToken) {
        try {
            ApiResponse<CoreRoomResponse> response = coreRestClient.get()
                    .uri("/internal/rental/rooms/{id}", roomId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
            }
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public CoreAccountResponse getCurrentAccount(String accessToken) {
        try {
            ApiResponse<CoreAccountResponse> response = coreRestClient.get()
                    .uri("/internal/rental/account")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public CoreLandlordContactResponse getLandlordContact(UUID landlordId, String accessToken) {
        try {
            ApiResponse<CoreLandlordContactResponse> response = coreRestClient.get()
                    .uri("/internal/rental/landlords/{landlordId}/contact", landlordId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public CoreLandlordDashboardResponse getLandlordDashboard(
            UUID landlordId,
            String accessToken) {
        try {
            ApiResponse<CoreLandlordDashboardResponse> response = coreRestClient.get()
                    .uri("/internal/rental/landlords/{landlordId}/dashboard", landlordId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public void sendContactCardNotification(
            UUID tenantId,
            UUID rentalRequestId,
            String accessToken) {
        ContactCardNotificationRequest request = ContactCardNotificationRequest.builder()
                .tenantId(tenantId)
                .rentalRequestId(rentalRequestId)
                .build();

        try {
            coreRestClient.post()
                    .uri("/internal/rental/notifications/contact-card")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public void sendNotification(
            CoreNotificationRequest request,
            String accessToken) {
        try {
            coreRestClient.post()
                    .uri("/internal/rental/notifications")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401
                    || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }
}
