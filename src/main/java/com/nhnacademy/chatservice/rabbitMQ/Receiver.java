package com.nhnacademy.chatservice.rabbitMQ;

import com.nhnacademy.chatservice.chat.service.ChatService;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.domain.Role;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Receiver {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;


    public Receiver(SimpMessageSendingOperations messageTemplate, ChatService chatService) {
        this.messageTemplate = messageTemplate;
        this.chatService = chatService;
    }

    public void receiveMessage(String message) {
        System.out.println("[#] Received: " + message);
        List<Member> members = chatService.findByRole_RoleName("ROLE_ADMIN");
        Role role = chatService.findByRoleName("ROLE_ADMIN");

        for(Member m : members) {
            System.out.println(m.getMbEmail());
            chatService.saveNotificationMessage(m, role, message);
//            messageTemplate.convertAndSend("/topic/" + m.getMbEmail(), message);
        }

//        messageTemplate.convertAndSend("/topic/"+25, message);
    }
}
