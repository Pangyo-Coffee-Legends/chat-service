package com.nhnacademy.chatservice.chat.repository;


import com.nhnacademy.chatservice.chat.domain.ChatMessage;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomId(Long chatRoomId);

    List<ChatMessage> findByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(ChatRoom chatRoom, LocalDateTime createdAt);
}
