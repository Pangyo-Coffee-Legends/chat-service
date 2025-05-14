package com.nhnacademy.chatservice.chat.repository;

import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.domain.MessageReadStatus;
import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
    Long countByChatRoomAndMemberAndIsReadFalse(ChatRoom chatRoom, Member member);

    List<MessageReadStatus> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
}
