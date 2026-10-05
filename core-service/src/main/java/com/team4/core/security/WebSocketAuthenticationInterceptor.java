package com.team4.core.security;

import com.team4.core.enums.UserStatus;
import com.team4.core.repositories.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Missing WebSocket access token");
        }

        var jwt = jwtTokenProvider.decode(authorization.substring(BEARER_PREFIX.length()));
        UUID userId = UUID.fromString(jwt.getSubject());
        boolean active = userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
        if (!active) {
            throw new BadCredentialsException("WebSocket account is not active");
        }

        var authorities = List.of(new SimpleGrantedAuthority(
                "ROLE_" + jwt.getClaimAsString("role")));
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId.toString(), null, authorities));
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
