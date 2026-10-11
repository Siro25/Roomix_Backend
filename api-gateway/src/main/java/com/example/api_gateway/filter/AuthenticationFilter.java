package com.example.api_gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private static final String API_PREFIX = "/api/";
    private static final String PUBLIC_AUTH_PREFIX = "/api/auth/";
    private static final String PUBLIC_ROOM_PREFIX = "/api/rooms/";
    private static final String WEB_SOCKET_ENDPOINT = "/api/ws";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if ("OPTIONS".equalsIgnoreCase(method)
                || !path.startsWith(API_PREFIX)
                || path.startsWith(PUBLIC_AUTH_PREFIX)
                || path.equals(WEB_SOCKET_ENDPOINT)
                || path.startsWith(WEB_SOCKET_ENDPOINT + "/")
                || ("GET".equalsIgnoreCase(method)
                        && path.startsWith(PUBLIC_ROOM_PREFIX))) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            return chain.filter(exchange);
        }

        byte[] body = "{\"error\":\"Missing Authorization header\"}"
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
