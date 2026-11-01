package com.connectx.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long userId,
        String username,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
