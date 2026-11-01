package com.connectx;

import com.connectx.dto.MessageResponse;
import com.connectx.dto.SendMessageRequest;
import com.connectx.service.MessageService;
import com.connectx.websocket.WebSocketMessageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketMessageControllerTest {


private MessageService messageService;
private SimpMessagingTemplate messagingTemplate;
private WebSocketMessageController controller;

@BeforeEach
void setUp() {
    messageService = mock(MessageService.class);
    messagingTemplate = mock(SimpMessagingTemplate.class);

    controller = new WebSocketMessageController(
            messageService, messagingTemplate);
}

@Test
void sendMessageShouldSaveAndDeliverToReceiver() {
    SendMessageRequest request = new SendMessageRequest();
    request.setReceiverId(2L);
    request.setContent("Hello Bob");

    Principal principal = () -> "1";

    MessageResponse savedMessage = new MessageResponse(
            10L, 1L, 2L, "Hello Bob", LocalDateTime.now());

    when(messageService.sendMessage(1L, request))
            .thenReturn(savedMessage);

    controller.sendMessage(request, principal);

    verify(messageService).sendMessage(1L, request);

    verify(messagingTemplate).convertAndSendToUser(
            eq("2"),
            eq("/queue/messages"),
            eq(savedMessage));
}

@Test
void sendMessageWithoutPrincipalShouldBeRejected() {
    SendMessageRequest request = new SendMessageRequest();
    request.setReceiverId(2L);
    request.setContent("Hello Bob");

    assertThrows(
            IllegalStateException.class,
            () -> controller.sendMessage(request, null));

    verify(messageService, never())
            .sendMessage(any(), any());

    verify(messagingTemplate, never())
            .convertAndSendToUser(any(String.class), any(String.class), any());
}


}
