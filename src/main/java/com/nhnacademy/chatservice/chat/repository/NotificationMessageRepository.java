package com.nhnacademy.chatservice.chat.repository;

import com.nhnacademy.chatservice.chat.domain.NotificationMessage;
import com.nhnacademy.chatservice.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
    Long countByMemberAndIsReadFalse(Member member);
}
