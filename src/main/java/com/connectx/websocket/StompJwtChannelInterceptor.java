
package com.connectx.websocket;

import com.connectx.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.util.List;

public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompJwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // Connection setup: authenticate the user from the JWT.
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(message, accessor);
            return message;
        }

        // Spring may generate DISCONNECT internally after the
        // WebSocket closes. It should not require a new principal.
        if (StompCommand.DISCONNECT.equals(command)) {
            return message;
        }

        // Ignore non-STOMP messages, such as heartbeats.
        if (command == null) {
            return message;
        }

        Principal user = accessor.getUser();

        if (user == null) {
            throw new MessageDeliveryException(
                    message, "Authentication is required");
        }

        String destination = accessor.getDestination();

        if (StompCommand.SEND.equals(command)) {
            if (!"/app/messages".equals(destination)) {
                throw new MessageDeliveryException(
                        message,
                        "Sending to this destination is not allowed");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            if (!"/user/queue/messages".equals(destination)) {
                throw new MessageDeliveryException(
                        message,
                        "Subscription to this destination is not allowed");
            }
        }

        return message;
    }

    private void authenticate(
            Message<?> message,
            StompHeaderAccessor accessor) {

        String authorization =
                accessor.getFirstNativeHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException(
                    message,
                    "Missing or invalid WebSocket Authorization header");
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty() || !jwtService.isTokenValid(token)) {
            throw new MessageDeliveryException(
                    message, "Invalid or expired JWT");
        }

        try {
            Long userId = jwtService.extractUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, List.of());

            accessor.setUser(authentication);

        } catch (JwtException | IllegalArgumentException exception) {
            throw new MessageDeliveryException(
                    message, "Invalid JWT identity");
        }
    }
}

