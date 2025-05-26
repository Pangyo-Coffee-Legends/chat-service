package com.nhnacademy.chatservice.chat.repository;

import com.nhnacademy.chatservice.chat.domain.ChatMessage;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.domain.MessageReadStatus;
import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
    List<MessageReadStatus> findByChatRoom(ChatRoom chatRoom);

    Long countByChatRoomAndChatMessageAndIsReadFalse(ChatRoom chatRoom, ChatMessage chatMessage);

    @Query(
            value = "SELECT m.message_id AS messageId, COUNT(*) AS unreadCount " +
                    "FROM message_read_status m " +
                    "WHERE m.chat_room_id = :roomId AND m.is_read = false " +
                    "GROUP BY m.message_id",
            nativeQuery = true
    )
    List<Map<String, Object>> findUnreadCountByRoomId(Long roomId);

    Long countByMemberAndIsReadFalse(Member member);

    Long countByChatRoomAndMemberAndIsReadFalse(ChatRoom chatRoom, Member member); // 이거 사용하자

    List<MessageReadStatus> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
}
