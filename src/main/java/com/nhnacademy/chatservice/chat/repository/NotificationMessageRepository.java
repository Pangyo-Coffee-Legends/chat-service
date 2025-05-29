package com.nhnacademy.chatservice.chat.repository;

import com.nhnacademy.chatservice.chat.domain.NotificationMessage;
import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    Long countByMemberAndIsReadFalse(Member member);

    List<NotificationMessage> findByMember(Member member);

    List<NotificationMessage> findByMemberAndIsReadFalse(Member member);
}
