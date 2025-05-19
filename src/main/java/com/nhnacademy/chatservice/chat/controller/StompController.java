package com.nhnacademy.chatservice.chat.controller;

import com.nhnacademy.chatservice.chat.domain.ChatMessage;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.repository.MessageReadStatusRepository;
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
    private final MessageReadStatusRepository readStatusRepository;

    private final ChatService chatService;

    public StompController(SimpMessageSendingOperations messageTemplate, MessageReadStatusRepository readStatusRepository, ChatService chatService) {
        this.messageTemplate = messageTemplate;
        this.readStatusRepository = readStatusRepository;
        this.chatService = chatService;
    }

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageDto) {
        log.info(chatMessageDto.getContent());

        // 메시지 받으면 DB에 저장하고, 구독(채팅방)하고 있는 사용자들한테 메시지를 보내야한다.

        // 메시지를 받으면 DB에 저장
        ChatMessage savedMessage = chatService.saveMessage(roomId, chatMessageDto);
        System.out.println(savedMessage.toString());
        ChatRoom chatRoom = savedMessage.getChatRoom();

        System.out.println("savedMessage = " + savedMessage.toString());

        Long unreadCount = readStatusRepository.countByChatRoomAndChatMessageAndIsReadFalse(chatRoom, savedMessage);

        ChatMessageDto dto = ChatMessageDto.builder()
                .id(savedMessage.getId())
                .sender(savedMessage.getMember().getMbEmail())
                .content(savedMessage.getContent())
                .createdAt(savedMessage.getCreatedAt())
                .unreadCount(unreadCount)
                .build();

        // 채팅방으로 메시지를 보내는 코드
        messageTemplate.convertAndSend("/topic/" + roomId, dto);
    }
}
