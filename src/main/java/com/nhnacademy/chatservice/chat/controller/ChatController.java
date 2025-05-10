package com.nhnacademy.chatservice.chat.controller;

import com.nhnacademy.chatservice.chat.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
//@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 그룹 채팅방 개설 / 참여자의 이메일은 헤더에서 받는걸로 변경
    @PostMapping("/room/group/create")
    public ResponseEntity<?> createGroupRoom(@RequestHeader("X-USER") String userEmail, @RequestParam String roomName) {
        chatService.createGroupRoom(userEmail, roomName);
        return ResponseEntity.ok().build();
    }

    // 내가 속해 있는 채팅 목록 조회
    @GetMapping("/room/list")
    public ResponseEntity<?> getChatRoomList(@RequestHeader("X-USER") String userEmail) {
        return ResponseEntity.ok(chatService.getChatRoomList(userEmail));
    }

    // 그룹 채팅방에 참여하는 api
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupRoom(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        chatService.addParticipantToGroupChat(roomId, userEmail);
        return ResponseEntity.ok().build();
    }
}
