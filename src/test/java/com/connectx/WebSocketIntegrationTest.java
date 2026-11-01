package com.connectx;

import com.connectx.repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MessageRepository messageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticatedUsersShouldExchangeMessagesOverWebSocket()
            throws Exception {

        // --------------------------------------------------
        // 1. Register and log in two users.
        // --------------------------------------------------

        String suffix = UUID.randomUUID().toString();

        TestUser sender = registerAndLogin(
                "sender_" + suffix.substring(0, 8),
                "sender_" + suffix + "@example.com",
                "Password123"
        );

        TestUser receiver = registerAndLogin(
                "receiver_" + suffix.substring(0, 8),
                "receiver_" + suffix + "@example.com",
                "Password123"
        );

        String messageContent = "Hello through WebSocket";

        ArrayBlockingQueue<JsonNode> receivedMessages =
                new ArrayBlockingQueue<>(1);

        // --------------------------------------------------
        // 2. Create the STOMP client.
        //
        // No Jackson STOMP converter is needed because
        // outgoing and incoming messages use byte arrays.
        // --------------------------------------------------

        WebSocketClient webSocketClient = new SockJsClient(
        List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        )
);

WebSocketStompClient stompClient =
        new WebSocketStompClient(webSocketClient);

        StompSession receiverSession = null;
        StompSession senderSession = null;

        try {

           String url = "http://localhost:" + port + "/ws";

            // --------------------------------------------------
            // 3. Connect the receiver using its JWT.
            // --------------------------------------------------

            StompHeaders receiverConnectHeaders =
                    new StompHeaders();

            receiverConnectHeaders.add(
                    "Authorization",
                    "Bearer " + receiver.token()
            );

            receiverSession = stompClient.connectAsync(
                    url,
                    new WebSocketHttpHeaders(),
                    receiverConnectHeaders,
                    new StompSessionHandlerAdapter() {

                        @Override
                        public void handleException(
                                StompSession session,
                                StompCommand command,
                                StompHeaders headers,
                                byte[] payload,
                                Throwable exception) {

                            System.err.println(
                                    "Receiver STOMP exception:"
                            );

                            exception.printStackTrace();
                        }

                        @Override
                        public void handleTransportError(
                                StompSession session,
                                Throwable exception) {

                            System.err.println(
                                    "Receiver WebSocket transport error:"
                            );

                            exception.printStackTrace();
                        }
                    }
            ).get(10, TimeUnit.SECONDS);

            assertTrue(
                    receiverSession.isConnected(),
                    "Receiver session should be connected"
            );

            // --------------------------------------------------
            // 4. Subscribe to the receiver's private queue.
            //
            // Wait until the subscription is processed by
            // allowing the sender connection to be established
            // after the receiver subscription has been sent.
            // --------------------------------------------------

            StompHeaders subscribeHeaders = new StompHeaders();

            subscribeHeaders.setDestination(
                    "/user/queue/messages"
            );

            receiverSession.subscribe(
                    subscribeHeaders,
                    new StompSessionHandlerAdapter() {

                        @Override
                        public Type getPayloadType(
                                StompHeaders headers) {

                            return byte[].class;
                        }

                        @Override
                        public void handleFrame(
                                StompHeaders headers,
                                Object payload) {

                            if (payload instanceof byte[] bytes) {

                                try {

                                    JsonNode json =
                                            objectMapper.readTree(bytes);

                                    receivedMessages.offer(json);

                                } catch (Exception exception) {

                                    System.err.println(
                                            "Failed to parse received "
                                                    + "WebSocket JSON:"
                                    );

                                    exception.printStackTrace();
                                }
                            }
                        }
                    }
            );

            // --------------------------------------------------
            // 5. Connect the sender using its JWT.
            // --------------------------------------------------

            StompHeaders senderConnectHeaders =
                    new StompHeaders();

            senderConnectHeaders.add(
                    "Authorization",
                    "Bearer " + sender.token()
            );

            senderSession = stompClient.connectAsync(
                    url,
                    new WebSocketHttpHeaders(),
                    senderConnectHeaders,
                    new StompSessionHandlerAdapter() {

                        @Override
                        public void handleException(
                                StompSession session,
                                StompCommand command,
                                StompHeaders headers,
                                byte[] payload,
                                Throwable exception) {

                            System.err.println(
                                    "Sender STOMP exception:"
                            );

                            exception.printStackTrace();
                        }

                        @Override
                        public void handleTransportError(
                                StompSession session,
                                Throwable exception) {

                            System.err.println(
                                    "Sender WebSocket transport error:"
                            );

                            exception.printStackTrace();
                        }
                    }
            ).get(10, TimeUnit.SECONDS);

            assertTrue(
                    senderSession.isConnected(),
                    "Sender session should be connected"
            );

            // --------------------------------------------------
            // 6. Send the message as JSON bytes.
            // --------------------------------------------------

            Map<String, Object> request = Map.of(
                    "receiverId", receiver.id(),
                    "content", messageContent
            );

            byte[] requestBytes =
                    objectMapper.writeValueAsBytes(request);

            StompHeaders sendHeaders = new StompHeaders();

            sendHeaders.setDestination("/app/messages");

            sendHeaders.setContentType(
                    MimeTypeUtils.APPLICATION_JSON
            );

            senderSession.send(
                    sendHeaders,
                    requestBytes
            );

            // --------------------------------------------------
            // 7. Wait for the receiver to get the message.
            // --------------------------------------------------

            JsonNode received = receivedMessages.poll(
                    10,
                    TimeUnit.SECONDS
            );

            // --------------------------------------------------
            // 8. Verify database persistence independently.
            // --------------------------------------------------

            boolean messageSaved =
                    messageRepository
                            .findByReceiverIdOrderByCreatedAtDesc(
                                    receiver.id()
                            )
                            .stream()
                            .anyMatch(message ->
                                    messageContent.equals(
                                            message.getContent()
                                    )
                            );

            System.out.println(
                    "DEBUG: Message received over WebSocket = "
                            + (received != null)
            );

            System.out.println(
                    "DEBUG: Message saved in database = "
                            + messageSaved
            );

            // --------------------------------------------------
            // 9. Assert successful message delivery.
            // --------------------------------------------------

            assertNotNull(
                    received,
                    "Receiver should receive the WebSocket message. "
                            + "Database saved = " + messageSaved
            );

            assertTrue(
                    received.has("senderId"),
                    "Response should contain senderId"
            );

            assertTrue(
                    received.has("receiverId"),
                    "Response should contain receiverId"
            );

            assertTrue(
                    received.has("content"),
                    "Response should contain content"
            );

            assertEquals(
                    sender.id().longValue(),
                    received.get("senderId").asLong()
            );

            assertEquals(
                    receiver.id().longValue(),
                    received.get("receiverId").asLong()
            );

            assertEquals(
                    messageContent,
                    received.get("content").asText()
            );

            assertTrue(
                    messageSaved,
                    "Message should be persisted in the database"
            );

        } finally {

            // --------------------------------------------------
            // 10. Clean up WebSocket sessions.
            // --------------------------------------------------

            if (senderSession != null
                    && senderSession.isConnected()) {

                senderSession.disconnect();
            }

            if (receiverSession != null
                    && receiverSession.isConnected()) {

                receiverSession.disconnect();
            }

            stompClient.stop();
        }
    }

    @Test
    void invalidJwtShouldNotAuthenticateWebSocketConnection()
            throws Exception {

        WebSocketStompClient stompClient =
                new WebSocketStompClient(
                        new StandardWebSocketClient()
                );

        try {

            String url = "ws://localhost:" + port + "/ws";

            StompHeaders headers = new StompHeaders();

            headers.add(
                    "Authorization",
                    "Bearer invalid-token"
            );

            assertThrows(
                    Exception.class,
                    () -> stompClient.connectAsync(
                            url,
                            new WebSocketHttpHeaders(),
                            headers,
                            new StompSessionHandlerAdapter() {
                            }
                    ).get(10, TimeUnit.SECONDS),

                    "Invalid JWT must not establish an "
                            + "authenticated STOMP session"
            );

        } finally {

            stompClient.stop();
        }
    }

    // --------------------------------------------------
    // Register a user and obtain a JWT using the REST API.
    // --------------------------------------------------

    private TestUser registerAndLogin(
            String username,
            String email,
            String password
    ) throws Exception {

        String baseUrl = "http://localhost:" + port;

        HttpClient httpClient = HttpClient.newHttpClient();

        // Register the user.

        Map<String, String> registrationRequest = Map.of(
                "username", username,
                "email", email,
                "password", password
        );

        HttpRequest registerRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                baseUrl + "/api/auth/register"
                        ))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        objectMapper.writeValueAsString(
                                                registrationRequest
                                        )
                                )
                        )
                        .build();

        HttpResponse<String> registrationResponse =
                httpClient.send(
                        registerRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(
                201,
                registrationResponse.statusCode(),
                "Registration should succeed: "
                        + registrationResponse.body()
        );

        // Log in and retrieve the JWT.

        Map<String, String> loginRequest = Map.of(
                "email", email,
                "password", password
        );

        HttpRequest loginHttpRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                baseUrl + "/api/auth/login"
                        ))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        objectMapper.writeValueAsString(
                                                loginRequest
                                        )
                                )
                        )
                        .build();

        HttpResponse<String> loginResponse =
                httpClient.send(
                        loginHttpRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(
                200,
                loginResponse.statusCode(),
                "Login should succeed: "
                        + loginResponse.body()
        );

        JsonNode json = objectMapper.readTree(
                loginResponse.body()
        );

        return new TestUser(
                json.get("id").asLong(),
                json.get("token").asText()
        );
    }

    private record TestUser(
            Long id,
            String token
    ) {
    }
}