package com.example.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTests {
    private final AuthenticationFilter filter = new AuthenticationFilter();
    private final String id = "a".repeat(43);

    private void allowed(String method, String path) {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.method(org.springframework.http.HttpMethod.valueOf(method),path).build());
        GatewayFilterChain chain=mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter.filter(exchange,chain).block();
        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
    private void blocked(String method, String path) {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.method(org.springframework.http.HttpMethod.valueOf(method),path).build());
        GatewayFilterChain chain=mock(GatewayFilterChain.class);
        filter.filter(exchange,chain).block();
        verifyNoInteractions(chain);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void anonymousFaceLoginCanCreateChallenge() { allowed("POST","/api/face/challenges"); }
    @Test void anonymousFaceLoginCanUploadFrame() { allowed("POST","/api/face/challenges/"+id+"/frames"); }
    @Test void anonymousFaceLoginCanRecoverStatus() { allowed("GET","/api/face/challenges/"+id); }
    @Test void unrelatedFaceRoutesStillNeedAuthorization() { blocked("GET","/api/face/profiles"); }
    @Test void challengeListingIsNotPublic() { blocked("GET","/api/face/challenges"); }
    @Test void deletingChallengeIsNotPublic() { blocked("DELETE","/api/face/challenges/"+id); }
    @Test void nestedRoutesCannotUseTheException() { blocked("POST","/api/face/challenges/"+id+"/frames/extra"); }
    @Test void adminRoutesStillNeedAuthorization() { blocked("GET","/api/admin/users"); }
    @Test void tenantRoutesStillNeedAuthorization() { blocked("GET","/api/favorites"); }
    @Test void loginAndRoomRoutesRemainPublic() {
        allowed("POST","/api/auth/login"); allowed("GET","/api/rooms/search");
    }
    @Test void existingAuthorizationIsForwardedUnchanged() {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").header("Authorization","Bearer test-only").build());
        GatewayFilterChain chain=mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter.filter(exchange,chain).block();
        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer test-only");
    }

    @Test void mainVnpayCallbacksRemainPublicForGetOnly() {
        allowed("GET", "/api/invoices/vnpay/ipn");
        allowed("GET", "/api/invoices/vnpay/return");
    }
    @Test void vnpayPostWithoutAuthorizationIsStillBlocked() {
        blocked("POST", "/api/invoices/vnpay/ipn");
        blocked("POST", "/api/invoices/vnpay/return");
    }
}
