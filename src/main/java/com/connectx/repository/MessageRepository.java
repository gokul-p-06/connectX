package com.connectx.repository;

import com.connectx.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.id = :userId AND m.receiver.id = :otherUserId)
               OR (m.sender.id = :otherUserId AND m.receiver.id = :userId)
            ORDER BY m.createdAt DESC
            """)
    List<Message> findConversation(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId
    );

    // Count unread messages from a specific sender
    long countByReceiverIdAndSenderIdAndReadFalse(
            Long receiverId,
            Long senderId
    );

    // Count all unread messages for a receiver
    long countByReceiverIdAndReadFalse(Long receiverId);

    // Mark messages from a specific sender as read
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Message m
            SET m.read = true
            WHERE m.receiver.id = :receiverId
              AND m.sender.id = :senderId
              AND m.read = false
            """)
    int markMessagesAsRead(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId
    );
}

