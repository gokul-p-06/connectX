package com.connectx.dto;

import com.connectx.entity.Message;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime createdAt;

    public MessageResponse() {
    }

    public MessageResponse(
            Long id,
            Long senderId,
            Long receiverId,
            String content,
            LocalDateTime createdAt) {

        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getReceiver().getId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}