package com.connectx.websocket;

import com.connectx.dto.MessageResponse;
import com.connectx.dto.SendMessageRequest;
import com.connectx.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Validated
public class WebSocketMessageController {


private final MessageService messageService;
private final SimpMessagingTemplate messagingTemplate;

public WebSocketMessageController(
        MessageService messageService,
        SimpMessagingTemplate messagingTemplate) {
    this.messageService = messageService;
    this.messagingTemplate = messagingTemplate;
}

@MessageMapping("/messages")
public void sendMessage(
        @Payload @Valid SendMessageRequest request,
        Principal principal) {

    if (principal == null) {
        throw new IllegalStateException(
                "Authentication is required to send messages");
    }

    Long senderId = Long.parseLong(principal.getName());

    MessageResponse response =
            messageService.sendMessage(senderId, request);

    messagingTemplate.convertAndSendToUser(
            response.getReceiverId().toString(),
            "/queue/messages",
            response);
}


}
