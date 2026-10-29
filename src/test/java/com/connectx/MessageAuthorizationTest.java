package com.connectx;

import com.connectx.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.connectx.repository.MessageRepository;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
private MessageRepository messageRepository;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private String createToken() {
        return jwtService.generateToken(1L);
    }

    private TestUser registerAndLogin(
            String username,
            String email,
            String password) throws Exception {

        String registrationRequest = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        MvcResult registrationResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registrationRequest))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString());

        Long userId = registrationJson.get("id").asLong();

        String loginRequest = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());

        String token = loginJson.get("token").asText();

        return new TestUser(userId, token);
    }

    private void sendMessage(
            String senderToken,
            Long receiverId,
            String content) throws Exception {

        String requestBody = """
                {
                    "receiverId": %d,
                    "content": "%s"
                }
                """.formatted(receiverId, content);

        mockMvc.perform(
                        post("/api/messages")
                                .header(
                                        "Authorization",
                                        "Bearer " + senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isCreated());
    }

    private JsonNode getMessages(
            String token,
            String endpoint) throws Exception {

        MvcResult result = mockMvc.perform(
                        get(endpoint)
                                .header(
                                        "Authorization",
                                        "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }

    private boolean containsContent(
            JsonNode messages,
            String expectedContent) {

        for (JsonNode message : messages) {
            JsonNode content = message.get("content");

            if (content != null
                    && expectedContent.equals(content.asText())) {
                return true;
            }
        }

        return false;
    }

    private record TestUser(Long id, String token) {
    }

    @Test
    void sendingMessageWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": 2,
                                    "content": "Hello Bob"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retrievingSentMessagesWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(get("/api/messages/sent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retrievingReceivedMessagesWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(get("/api/messages/received"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendingMessageWithBlankContentWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": 2,
                                    "content": "   "
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendingMessageWithInvalidReceiverWithoutAuthenticationShouldReturn401()
            throws Exception {

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": -1,
                                    "content": "Hello Bob"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendingMessageWithBlankContentAndAuthenticationShouldReturn400()
            throws Exception {

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": 2,
                                    "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Message content cannot be empty"));
    }

    @Test
    void sendingMessageWithNegativeReceiverAndAuthenticationShouldReturn400()
            throws Exception {

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": -1,
                                    "content": "Hello Bob"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendingMessageExceeding2000CharactersAndAuthenticationShouldReturn400()
            throws Exception {

        String content = "a".repeat(2001);

        String requestBody = """
                {
                    "receiverId": 2,
                    "content": "%s"
                }
                """.formatted(content);

        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aliceCanSendMessageAndBobCanReceiveIt() throws Exception {

        TestUser alice = registerAndLogin(
                "alice",
                "alice.integration@example.com",
                "AlicePassword123");

        TestUser bob = registerAndLogin(
                "bob",
                "bob.integration@example.com",
                "BobPassword123");

        sendMessage(
                alice.token(),
                bob.id(),
                "Hello Bob from Alice");

        JsonNode bobReceivedMessages = getMessages(
                bob.token(),
                "/api/messages/received");

        assertTrue(
                containsContent(
                        bobReceivedMessages,
                        "Hello Bob from Alice"),
                "Bob should receive Alice's message");
    }

    @Test
    void aliceCanRetrieveHerSentMessage() throws Exception {

        TestUser alice = registerAndLogin(
                "alice",
                "alice.sent@example.com",
                "AlicePassword123");

        TestUser bob = registerAndLogin(
                "bob",
                "bob.sent@example.com",
                "BobPassword123");

        sendMessage(
                alice.token(),
                bob.id(),
                "Alice sent this message");

        JsonNode aliceSentMessages = getMessages(
                alice.token(),
                "/api/messages/sent");

        assertTrue(
                containsContent(
                        aliceSentMessages,
                        "Alice sent this message"),
                "Alice should see her sent message");
    }

    @Test
    void usersShouldOnlySeeMessagesBelongingToTheirOwnLists()
            throws Exception {

        TestUser alice = registerAndLogin(
                "alice",
                "alice.ownership@example.com",
                "AlicePassword123");

        TestUser bob = registerAndLogin(
                "bob",
                "bob.ownership@example.com",
                "BobPassword123");

        sendMessage(
                alice.token(),
                bob.id(),
                "Alice to Bob");

        sendMessage(
                bob.token(),
                alice.id(),
                "Bob to Alice");

        JsonNode aliceSentMessages = getMessages(
                alice.token(),
                "/api/messages/sent");

        JsonNode aliceReceivedMessages = getMessages(
                alice.token(),
                "/api/messages/received");

        JsonNode bobSentMessages = getMessages(
                bob.token(),
                "/api/messages/sent");

        JsonNode bobReceivedMessages = getMessages(
                bob.token(),
                "/api/messages/received");

        assertTrue(
                containsContent(aliceSentMessages, "Alice to Bob"));

        assertFalse(
                containsContent(aliceSentMessages, "Bob to Alice"),
                "Alice's sent list must not contain Bob's message");

        assertTrue(
                containsContent(aliceReceivedMessages, "Bob to Alice"));

        assertFalse(
                containsContent(aliceReceivedMessages, "Alice to Bob"),
                "Alice's received list must not contain her outgoing message");

        assertTrue(
                containsContent(bobSentMessages, "Bob to Alice"));

        assertFalse(
                containsContent(bobSentMessages, "Alice to Bob"),
                "Bob's sent list must not contain Alice's message");

        assertTrue(
                containsContent(bobReceivedMessages, "Alice to Bob"));

        assertFalse(
                containsContent(bobReceivedMessages, "Bob to Alice"),
                "Bob's received list must not contain his outgoing message");
                
    }
    
@Test
void sendingMessageToNonexistentReceiverShouldReturn404()
throws Exception {

TestUser alice = registerAndLogin(
        "alice",
        "alice.nonexistent@example.com",
        "AlicePassword123");

long nonexistentReceiverId = 99999L;

long messageCountBefore = messageRepository.count();

mockMvc.perform(
                post("/api/messages")
                        .header(
                                "Authorization",
                                "Bearer " + alice.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": %d,
                                    "content": "Hello nonexistent user"
                                }
                                """.formatted(nonexistentReceiverId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Receiver not found"));

long messageCountAfter = messageRepository.count();

org.junit.jupiter.api.Assertions.assertEquals(
        messageCountBefore,
        messageCountAfter,
        "No message should be saved when the receiver does not exist");


}

@Test
void userWithNoMessagesShouldReceiveEmptySentAndReceivedLists()
        throws Exception {

    TestUser alice = registerAndLogin(
            "alice",
            "alice.empty-lists@example.com",
            "AlicePassword123");

    JsonNode sentMessages = getMessages(
            alice.token(),
            "/api/messages/sent");

    JsonNode receivedMessages = getMessages(
            alice.token(),
            "/api/messages/received");

    assertTrue(
            sentMessages.isArray(),
            "Sent messages response should be a JSON array");

    assertTrue(
            sentMessages.isEmpty(),
            "Sent messages list should be empty");

    assertTrue(
            receivedMessages.isArray(),
            "Received messages response should be a JSON array");

    assertTrue(
            receivedMessages.isEmpty(),
            "Received messages list should be empty");
}
@Test
void sendingMessageWithSurroundingSpacesShouldTrimContent()
        throws Exception {

    TestUser alice = registerAndLogin(
            "alice",
            "alice.trim@example.com",
            "AlicePassword123");

    TestUser bob = registerAndLogin(
            "bob",
            "bob.trim@example.com",
            "BobPassword123");

    sendMessage(
            alice.token(),
            bob.id(),
            "   Hello Bob   ");

    JsonNode sentMessages = getMessages(
            alice.token(),
            "/api/messages/sent");

    assertTrue(
            containsContent(sentMessages, "Hello Bob"),
            "Message content should have leading and trailing spaces removed");

    assertFalse(
            containsContent(sentMessages, "   Hello Bob   "),
            "Message content should not retain surrounding spaces");
}
@Test
void sendingMessageWithoutReceiverIdShouldReturn400()
        throws Exception {

    TestUser alice = registerAndLogin(
            "alice",
            "alice.missing-receiver@example.com",
            "AlicePassword123");

    long messageCountBefore = messageRepository.count();

    mockMvc.perform(
                    post("/api/messages")
                            .header(
                                    "Authorization",
                                    "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "content": "Hello Bob"
                                    }
                                    """))
            .andExpect(status().isBadRequest());

    long messageCountAfter = messageRepository.count();

    org.junit.jupiter.api.Assertions.assertEquals(
            messageCountBefore,
            messageCountAfter,
            "No message should be saved when receiver ID is missing");
}
@Test
void sendingMessageWithoutContentShouldReturn400()
        throws Exception {

    TestUser alice = registerAndLogin(
            "alice",
            "alice.missing-content@example.com",
            "AlicePassword123");

    TestUser bob = registerAndLogin(
            "bob",
            "bob.missing-content@example.com",
            "BobPassword123");

    long messageCountBefore = messageRepository.count();

    mockMvc.perform(
                    post("/api/messages")
                            .header(
                                    "Authorization",
                                    "Bearer " + alice.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "receiverId": %d
                                    }
                                    """.formatted(bob.id())))
            .andExpect(status().isBadRequest());

    long messageCountAfter = messageRepository.count();

    org.junit.jupiter.api.Assertions.assertEquals(
            messageCountBefore,
            messageCountAfter,
            "No message should be saved when content is missing");
}
@Test
void loginWithInvalidPasswordShouldReturn401() throws Exception {

    registerAndLogin(
            "alice",
            "alice.invalid-login@example.com",
            "AlicePassword123");

    mockMvc.perform(
                    post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "alice.invalid-login@example.com",
                                        "password": "WrongPassword123"
                                    }
                                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error")
                    .value("Invalid email or password"));
}

@Test
void registeringWithDuplicateEmailShouldReturn409() throws Exception {

    String registrationRequest = """
            {
                "username": "alice",
                "email": "alice.duplicate@example.com",
                "password": "AlicePassword123"
            }
            """;

    mockMvc.perform(
                    post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationRequest))
            .andExpect(status().isCreated());

    mockMvc.perform(
                    post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationRequest))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error")
                    .value("Email is already registered"));
}
}