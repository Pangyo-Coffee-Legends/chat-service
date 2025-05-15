package com.nhnacademy.chatservice.chat.service;

import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.dto.ChatRoomListResDto;
import com.nhnacademy.chatservice.chat.dto.EmailListRequestDto;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.dto.MemberDto;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface ChatService {

    void saveMessage(Long roomId, ChatMessageDto chatMessageDto);

    void createGroupRoom(String userEmail, String roomName);

    List<ChatRoomListResDto> getChatRoomList(String email);

    void addParticipantToGroupChat(Long roomId, String userEmail);

    void addParticipant(ChatRoom room, Member member);

    List<ChatMessageDto> getHistory(String userEmail, Long roomId);

    boolean isRoomParticipant(String email, Long roomId);

    void messageRead(Long roomId, String userEmail);

    void leaveChatRoom(Long roomId, String userEmail);

    void addParticipantsToGroupChat(EmailListRequestDto emailListRequestDto, Long roomId);

    List<MemberDto> getNonChatRoomMembers(Long roomId);

    Long getUnreadCount(String email);
}
