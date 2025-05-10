package com.nhnacademy.chatservice.chat.repository;


import com.nhnacademy.chatservice.chat.domain.ChatParticipant;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);

    int countByChatRoom(ChatRoom chatRoom);

    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
}
