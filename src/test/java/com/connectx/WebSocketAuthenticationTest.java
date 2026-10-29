package com.connectx;

import com.connectx.service.JwtService;
import com.connectx.websocket.StompJwtChannelInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebSocketAuthenticationTest {

private JwtService jwtService;
private StompJwtChannelInterceptor interceptor;
private MessageChannel channel;

@BeforeEach
void setUp() {
    jwtService = new JwtService(
            "VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JDb25uZWN0WFhY",
            900000L);

    interceptor = new StompJwtChannelInterceptor(jwtService);
    channel = mock(MessageChannel.class);
}

private Message<byte[]> connectMessage(String token) {
    StompHeaderAccessor accessor =
            StompHeaderAccessor.create(StompCommand.CONNECT);

    if (token != null) {
        accessor.setNativeHeader("Authorization", "Bearer " + token);
    }

    accessor.setLeaveMutable(true);

    return MessageBuilder.createMessage(
            new byte[0], accessor.getMessageHeaders());
}

@Test
void connectWithoutTokenShouldBeRejected() {
    Message<byte[]> message = connectMessage(null);

    assertThrows(
            MessageDeliveryException.class,
            () -> interceptor.preSend(message, channel));
}

@Test
void connectWithInvalidTokenShouldBeRejected() {
    Message<byte[]> message = connectMessage("invalid-token");

    assertThrows(
            MessageDeliveryException.class,
            () -> interceptor.preSend(message, channel));
}

@Test
void connectWithValidTokenShouldSetAuthenticatedUser() {
    String token = jwtService.generateToken(42L);
    Message<byte[]> message = connectMessage(token);

    Message<?> result = interceptor.preSend(message, channel);

    assertNotNull(result);

    StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(result);

    assertNotNull(accessor.getUser());
    assertEquals("42", accessor.getUser().getName());
    assertTrue(accessor.getUser().getName().matches("\\d+"));
}
private Message<byte[]> authenticatedMessage(
StompCommand command,
String destination,
String userId) {


StompHeaderAccessor accessor =
        StompHeaderAccessor.create(command);

accessor.setDestination(destination);
accessor.setUser(
        new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(
                userId, null, List.of()));

accessor.setLeaveMutable(true);

return MessageBuilder.createMessage(
        new byte[0], accessor.getMessageHeaders());


}

@Test
void sendingDirectlyToBrokerDestinationShouldBeRejected() {
Message<byte[]> message = authenticatedMessage(
StompCommand.SEND, "/queue/messages", "42");


assertThrows(
        MessageDeliveryException.class,
        () -> interceptor.preSend(message, channel));


}

@Test
void subscribingToTopicDestinationShouldBeRejected() {
Message<byte[]> message = authenticatedMessage(
StompCommand.SUBSCRIBE, "/topic/messages", "42");


assertThrows(
        MessageDeliveryException.class,
        () -> interceptor.preSend(message, channel));


}

@Test
void sendingToApplicationDestinationShouldBeAllowed() {
Message<byte[]> message = authenticatedMessage(
StompCommand.SEND, "/app/messages", "42");


Message<?> result = interceptor.preSend(message, channel);

assertNotNull(result);


}

@Test
void subscribingToUserMessageQueueShouldBeAllowed() {
Message<byte[]> message = authenticatedMessage(
StompCommand.SUBSCRIBE, "/user/queue/messages", "42");


Message<?> result = interceptor.preSend(message, channel);

assertNotNull(result);


}

}
