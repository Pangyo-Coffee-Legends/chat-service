package com.nhnacademy.chatservice.chat.service;

import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.dto.ChatRoomListResDto;
import com.nhnacademy.chatservice.member.domain.Member;

import java.util.List;

public interface ChatService {

    void saveMessage(Long roomId, ChatMessageDto chatMessageDto);

    void createGroupRoom(String userEmail, String roomName);

    List<ChatRoomListResDto> getChatRoomList(String email);

    void addParticipantToGroupChat(Long roomId, String userEmail);

    void addParticipant(ChatRoom room, Member member);
}
