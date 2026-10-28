package com.connectx;

import com.connectx.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String createToken() {
        return jwtService.generateToken(1L);
    }

    @Test
    void sendingMessageWithoutAuthenticationShouldReturn401() throws Exception {
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
    void retrievingSentMessagesWithoutAuthenticationShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/messages/sent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retrievingReceivedMessagesWithoutAuthenticationShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/messages/received"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendingMessageWithBlankContentWithoutAuthenticationShouldReturn401() throws Exception {
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
    void sendingMessageWithInvalidReceiverWithoutAuthenticationShouldReturn401() throws Exception {
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
    void sendingMessageWithBlankContentAndAuthenticationShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverId": 2,
                                    "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendingMessageWithNegativeReceiverAndAuthenticationShouldReturn400() throws Exception {
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
    void sendingMessageExceeding2000CharactersAndAuthenticationShouldReturn400() throws Exception {
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
}