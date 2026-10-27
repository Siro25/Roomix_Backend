package com.example.api_gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayFaceRoutingTests {
    private static final HttpServer upstream = startUpstream();
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newHttpClient();

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                byte[] body = (exchange.getRequestURI().getPath() + "|" + exchange.getRequestMethod()
                        + "|" + (authorization == null ? "anonymous" : authorization)).getBytes(StandardCharsets.UTF_8);
                exchange.getRequestBody().readAllBytes();
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                if (origin != null) exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot start local upstream test server", exception);
        }
    }
    @DynamicPropertySource static void urls(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + upstream.getAddress().getPort();
        registry.add("CORE_SERVICE_URL", () -> url);
        registry.add("FACE_SERVICE_URL", () -> url);
    }
    @AfterAll static void closeUpstream() { upstream.stop(0); }
    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(java.time.Duration.ofSeconds(10));
    }
    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void anonymousFaceChallengeReachesFaceService() throws Exception {
        var result = send(request("/api/face/challenges").POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).isEqualTo("/api/face/challenges|POST|anonymous");
    }
    @Test void anonymousFrameUploadReachesFaceService() throws Exception {
        String path = "/api/face/challenges/" + "a".repeat(43) + "/frames";
        var result = send(request(path).POST(HttpRequest.BodyPublishers.ofByteArray(new byte[]{1,2,3})));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).isEqualTo(path + "|POST|anonymous");
    }
    @Test void enrollmentAuthorizationIsForwardedToFace() throws Exception {
        var result = send(request("/api/face/challenges").header("Authorization", "Bearer test-only")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).endsWith("Bearer test-only");
    }
    @Test void coreFaceSessionEndpointIsRoutedToCore() throws Exception {
        var result = send(request("/api/auth/face/sessions").POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).startsWith("/api/auth/face/sessions|");
    }
    @Test void frontendPreflightAllowsAuthorizationAndBinaryUpload() throws Exception {
        var result = send(request("/api/face/challenges").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.headers().firstValue("access-control-allow-origin")).contains("http://localhost:3000");
        assertThat(result.headers().firstValue("access-control-allow-credentials")).contains("true");
    }
    @Test void duplicateCorsHeadersFromCoreAreDeduplicated() throws Exception {
        var result = send(request("/api/auth/face/sessions").header("Origin", "http://localhost:3000")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.headers().allValues("access-control-allow-origin")).containsExactly("http://localhost:3000");
    }
    @Test void unconfiguredOriginIsRejected() throws Exception {
        var result = send(request("/api/face/challenges").header("Origin", "https://untrusted.test")
                .header("Access-Control-Request-Method", "POST").method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertThat(result.statusCode()).isEqualTo(403);
    }
    @Test void internalFaceCallbacksAreNotRoutedFromGateway() throws Exception {
        var result = send(request("/internal/face/results").POST(HttpRequest.BodyPublishers.ofString("{}")));
        assertThat(result.statusCode()).isEqualTo(404);
    }
}
