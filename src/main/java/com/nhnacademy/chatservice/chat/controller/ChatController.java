package com.nhnacademy.chatservice.chat.controller;

import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.dto.EmailListRequestDto;
import com.nhnacademy.chatservice.chat.service.ChatService;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.dto.MemberDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 채팅방에 없는 회원 조회
    @GetMapping("/room/{roomId}/nonmember/list")
    public ResponseEntity<?> getNonChatRoomMembers(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        List<MemberDto> members = chatService.getNonChatRoomMembers(roomId);
        return ResponseEntity.ok(members);
    }

    // 채팅방에 참여하는 api
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupRoom(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        chatService.addParticipantToGroupChat(roomId, userEmail);
        return ResponseEntity.ok().build();
    }

    // 채팅방에 초대하는 api
    @PostMapping("/room/group/invite-multiple/{roomId}/join")
    public ResponseEntity<?> inviteGroupRoom(@RequestBody EmailListRequestDto emailListRequestDto, @PathVariable Long roomId) {
        System.out.println("안녕" + emailListRequestDto);
        chatService.addParticipantsToGroupChat(emailListRequestDto, roomId);
        return ResponseEntity.ok().build();
    }

//    // 여러명을 채팅방에 초대하는 api
//    @PostMapping("/room/group/invite-multiple/{roomId}/join")
//    public ResponseEntity<?> inviteMembersGroupRoom(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
//        chatService.addParticipantToGroupChat(roomId, userEmail);
//        return ResponseEntity.ok().build();
//    }

    // 특정 채팅방의 이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getHistory(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        List<ChatMessageDto> chatMessageDtos = chatService.getHistory(userEmail, roomId);
        return ResponseEntity.ok(chatMessageDtos);
    }

    // 특정 채팅방의 채팅 메시지 읽음 처리
    @PostMapping("/room/{roomId}/read")
    public ResponseEntity<?> messageRead(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        chatService.messageRead(roomId, userEmail);
        return ResponseEntity.ok().build();
    }

    // 특정 채팅방 나가기 처리
    @DeleteMapping("/room/{roomId}/leave")
    public ResponseEntity<?> leaveChatRoom(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        chatService.leaveChatRoom(roomId, userEmail);
        return ResponseEntity.ok().build();
    }

    // 내가 읽지 않은 메시지의 총 개수
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("X-USER") String userEmail) {
        return ResponseEntity.ok(chatService.getUnreadCount(userEmail));
    }

    // 채팅방 입장 시 채팅방에 존재하는 모든 사용자의 메시지 요소 중 하나인 unreadCount를 갱신
    @GetMapping("/room/{roomId}/unreadUpdate")
    public ResponseEntity<?> sendUnreadCountUpdate(@RequestHeader("X-USER") String userEmail, @PathVariable Long roomId) {
        chatService.sendUnreadCountUpdate(roomId);
        return ResponseEntity.ok().build();
    }
}
