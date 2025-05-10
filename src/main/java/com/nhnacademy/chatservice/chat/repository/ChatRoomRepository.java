package com.nhnacademy.chatservice.chat.repository;


import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByIsGroupChat(String isGroupChat);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.chatParticipants cp JOIN cp.member m WHERE m.mbEmail = :email")
    List<ChatRoom> findChatRoomsByMemberEmail(@Param("email") String email);
}
