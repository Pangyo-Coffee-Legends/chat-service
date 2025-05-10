package com.nhnacademy.chatservice.chat.controller;

import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class StompController {

    private final SimpMessageSendingOperations messageTemplate;

    private final ChatService chatService;

    public StompController(SimpMessageSendingOperations messageTemplate, ChatService chatService) {
        this.messageTemplate = messageTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageDto) {
        log.info(chatMessageDto.getContent());

        // 메시지 받으면 DB에 저장하고, 구독(채팅방)하고 있는 사용자들한테 메시지를 보내야한다.

        // 메시지를 받으면 DB에 저장
        chatService.saveMessage(roomId, chatMessageDto);

        // 채팅방으로 보내는 코드
        messageTemplate.convertAndSend("/topic/" + roomId, chatMessageDto);
    }
}
