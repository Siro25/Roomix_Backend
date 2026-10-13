package com.team4.rental.external;

import com.team4.rental.dtos.response.*;
import com.team4.rental.exceptions.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
@RequiredArgsConstructor
public class CoreServiceClient {
    private final RestClient coreRestClient;

    public CoreRoomResponse getRoom(UUID roomId, String accessToken) {
        try {
            var response = coreRestClient.get().uri("/internal/rental/rooms/{id}", roomId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve().body(new ParameterizedTypeReference<ApiResponse<CoreRoomResponse>>() {});
            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new AppException(ErrorCode.ROOM_UNAVAILABLE);
            }
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }

    public CoreAccountResponse getCurrentAccount(String accessToken) {
        try {
            var response = coreRestClient.get().uri("/internal/rental/account")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve().body(new ParameterizedTypeReference<ApiResponse<CoreAccountResponse>>() {});
            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.CORE_UNAVAILABLE);
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.CORE_UNAVAILABLE);
        }
    }
}
