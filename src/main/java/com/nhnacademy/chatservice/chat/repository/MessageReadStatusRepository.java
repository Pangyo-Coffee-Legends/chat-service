package com.nhnacademy.chatservice.chat.repository;

import com.nhnacademy.chatservice.chat.domain.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

}
