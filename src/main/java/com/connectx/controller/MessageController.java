package com.connectx.controller;

import com.connectx.dto.MessageResponse;
import com.connectx.dto.SendMessageRequest;
import com.connectx.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.connectx.dto.ConversationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @Valid @RequestBody SendMessageRequest request) {

        Long userId = getAuthenticatedUserId();

        return messageService.sendMessage(userId, request);
    }

    @GetMapping("/received")
    public List<MessageResponse> getReceivedMessages() {

        Long userId = getAuthenticatedUserId();

        return messageService.getReceivedMessages(userId);
    }

    @GetMapping("/sent")
    public List<MessageResponse> getSentMessages() {

        Long userId = getAuthenticatedUserId();

        return messageService.getSentMessages(userId);
    }
    @GetMapping("/conversations")
public List<ConversationResponse> getConversations() {

    Long userId = getAuthenticatedUserId();

    return messageService.getConversations(userId);
}

    private Long getAuthenticatedUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return Long.valueOf(authentication.getName());
    }
    @GetMapping("/unread/{senderId}")
public long getUnreadCount(
        @org.springframework.web.bind.annotation.PathVariable Long senderId) {

    Long receiverId = getAuthenticatedUserId();

    return messageService.getUnreadCount(receiverId, senderId);
}

@org.springframework.web.bind.annotation.PutMapping(
        "/read/{senderId}")
public int markMessagesAsRead(
        @org.springframework.web.bind.annotation.PathVariable Long senderId) {

    Long receiverId = getAuthenticatedUserId();

    return messageService.markMessagesAsRead(receiverId, senderId);
}
}