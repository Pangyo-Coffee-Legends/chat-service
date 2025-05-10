package com.nhnacademy.chatservice.chat.service.impl;

import com.nhnacademy.chatservice.chat.domain.ChatMessage;
import com.nhnacademy.chatservice.chat.domain.ChatParticipant;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.domain.MessageReadStatus;
import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.dto.ChatRoomListResDto;
import com.nhnacademy.chatservice.chat.repository.ChatMessageRepository;
import com.nhnacademy.chatservice.chat.repository.ChatParticipantRepository;
import com.nhnacademy.chatservice.chat.repository.ChatRoomRepository;
import com.nhnacademy.chatservice.chat.repository.MessageReadStatusRepository;
import com.nhnacademy.chatservice.chat.service.ChatService;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;

    public ChatServiceImpl(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, MessageReadStatusRepository readStatusRepository, MemberRepository memberRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.memberRepository = memberRepository;
    }

    // 메시지 저장
    @Override
    public void saveMessage(Long roomId, ChatMessageDto chatMessageDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                ()-> new EntityNotFoundException("존재하지 않는 채팅방입니다."));
        // 메시지를 보낸 회원 조회
        Member sender = memberRepository.findByMbEmail(chatMessageDto.getSender()).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        
        // 메세지 테이블에 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sender)
                .content(chatMessageDto.getContent())
                .build();

        chatMessageRepository.save(chatMessage);
        
        // 채팅방에 존재하는 참여자들의 메시지 읽음 테이블에 읽음 여부를 저장
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);

        for (ChatParticipant chatParticipant : chatParticipants) {
            MessageReadStatus messageReadStatus = MessageReadStatus.builder()
                    .chatRoom(chatRoom)
                    .member(chatParticipant.getMember())
                    .chatMessage(chatMessage)
                    .isRead((chatParticipant.getMember().equals(sender)))
                    .build();

            readStatusRepository.save(messageReadStatus);
        }
    }


    // 그룹 채팅방 개설 / 참여자의 이메일은 헤더에서 받는걸로 변경
    @Override
    public void createGroupRoom(String userEmail, String roomName) {
        Member member= memberRepository.findByMbEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .isGroupChat("Y")
                .build();

        chatRoomRepository.save(chatRoom);

        // 참여자 생성
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    // 내가 속한 채팅방 조회
    @Override
    public List<ChatRoomListResDto> getChatRoomList(String email) {
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByMemberEmail(email);
        List<ChatRoomListResDto> chatRoomList = new ArrayList<>();

        for(ChatRoom chatRoom : chatRooms) {
            int participantCount = chatParticipantRepository.countByChatRoom(chatRoom);
            ChatRoomListResDto dto = ChatRoomListResDto.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .participantCount(participantCount)
                    .build();
            chatRoomList.add(dto);
        }

        return chatRoomList;
    }

    // 해당 그룹 채팅방에 참여자 추가
    @Override
    public void addParticipantToGroupChat(Long roomId, String userEmail) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        Member member = memberRepository.findByMbEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        Optional<ChatParticipant> chatParticipant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);

        // 참여하려고 하는 멤버가 참여자가 아닌 경우 새로 등록해주는 로직
        if (!chatParticipant.isPresent()) {
            addParticipant(chatRoom, member);
        }
    }

    // 참여자 추가
    @Override
    public void addParticipant(ChatRoom room, Member member) {
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(room)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }
}
