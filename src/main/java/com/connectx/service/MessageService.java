package com.connectx.service;

import com.connectx.dto.MessageResponse;
import com.connectx.dto.SendMessageRequest;
import com.connectx.entity.Message;
import com.connectx.entity.User;
import com.connectx.repository.MessageRepository;
import com.connectx.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.connectx.dto.ConversationResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageResponse sendMessage(
            Long senderId,
            SendMessageRequest request) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sender not found"
                ));

        User receiver = userRepository.findById(
                        request.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receiver not found"
                ));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent().trim());
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        return MessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getReceivedMessages(Long userId) {

        return messageRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getSentMessages(Long userId) {

        return messageRepository
                .findBySenderIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
public List<ConversationResponse> getConversations(Long userId) {

    List<Message> messages = messageRepository.findAll().stream()
            .filter(message ->
                    message.getSender().getId().equals(userId)
                    || message.getReceiver().getId().equals(userId))
            .filter(message ->
                    !message.getSender().getId()
                            .equals(message.getReceiver().getId()))
            .toList();

    return messages.stream()
            .collect(java.util.stream.Collectors.toMap(
                    message -> message.getSender().getId().equals(userId)
                            ? message.getReceiver()
                            : message.getSender(),
                    message -> message,
                    (first, second) ->
                            first.getCreatedAt().isAfter(second.getCreatedAt())
                                    ? first : second
            ))
            .entrySet()
            .stream()
            .map(entry -> new ConversationResponse(
        entry.getKey().getId(),
        entry.getKey().getUsername(),
        entry.getValue().getContent(),
        entry.getValue().getCreatedAt(),
        messageRepository.countByReceiverIdAndSenderIdAndReadFalse(
                userId,
                entry.getKey().getId()
        )
))
            .sorted((a, b) ->
                    b.lastMessageAt().compareTo(a.lastMessageAt()))
            .toList();
}
@Transactional(readOnly = true)
public long getUnreadCount(Long receiverId, Long senderId) {
    return messageRepository
            .countByReceiverIdAndSenderIdAndReadFalse(
                    receiverId, senderId);
}

@Transactional
public int markMessagesAsRead(Long receiverId, Long senderId) {
    return messageRepository.markMessagesAsRead(
            receiverId, senderId);
}
}   