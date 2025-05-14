package com.nhnacademy.chatservice.chat.service.impl;

import com.nhnacademy.chatservice.chat.domain.ChatMessage;
import com.nhnacademy.chatservice.chat.domain.ChatParticipant;
import com.nhnacademy.chatservice.chat.domain.ChatRoom;
import com.nhnacademy.chatservice.chat.domain.MessageReadStatus;
import com.nhnacademy.chatservice.chat.dto.ChatMessageDto;
import com.nhnacademy.chatservice.chat.dto.ChatRoomListResDto;
import com.nhnacademy.chatservice.chat.dto.EmailListRequestDto;
import com.nhnacademy.chatservice.chat.repository.ChatMessageRepository;
import com.nhnacademy.chatservice.chat.repository.ChatParticipantRepository;
import com.nhnacademy.chatservice.chat.repository.ChatRoomRepository;
import com.nhnacademy.chatservice.chat.repository.MessageReadStatusRepository;
import com.nhnacademy.chatservice.chat.service.ChatService;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.dto.MemberDto;
import com.nhnacademy.chatservice.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.member;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final SimpMessageSendingOperations messageTemplate;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;

    public ChatServiceImpl(SimpMessageSendingOperations messageTemplate, ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, MessageReadStatusRepository readStatusRepository, MemberRepository memberRepository) {
        this.messageTemplate = messageTemplate;
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
        Member member = memberRepository.findByMbEmail(email).orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByMemberEmail(email);
        List<ChatRoomListResDto> chatRoomList = new ArrayList<>();

        for(ChatRoom chatRoom : chatRooms) {
            Long count = readStatusRepository.countByChatRoomAndMemberAndIsReadFalse(chatRoom, member);
            int participantCount = chatParticipantRepository.countByChatRoom(chatRoom);
            ChatRoomListResDto dto = ChatRoomListResDto.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .participantCount(participantCount)
                    .unreadCount(count)
                    .build();
            chatRoomList.add(dto);
        }

        return chatRoomList;
    }

    // 해당 채팅방에 없는 멤버 조회
    public List<MemberDto> getNonChatRoomMembers(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        List<Member> members = memberRepository.findMembersNotInChatRoom(roomId);
        List<MemberDto> memberDtos = new ArrayList<>();

        for(Member member : members) {
            MemberDto dto = MemberDto.builder()
                    .name(member.getMbName())
                    .email(member.getMbEmail())
                    .build();

            memberDtos.add(dto);
        }
        return memberDtos;
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
            // 퇴장 알림 메시지 생성 및 발송
            String inviteMessageContent = userEmail + " 님을 채팅방에 나갔습니다."; // 또는 member.getName() 사용
            ChatMessageDto inviteNotification = ChatMessageDto.builder()
                    .sender("system") // 또는 나간 사용자 이메일/이름
                    .content(inviteMessageContent)
                    .build();

            addParticipant(chatRoom, member);
        }
    }

    // 해당 그룹 채팅방에 참여자들 추가
    @Override
    public void addParticipantsToGroupChat(EmailListRequestDto emailListRequestDto, Long roomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("채팅방이 존재하지 않습니다."));

        List<String> emails = emailListRequestDto.getEmails();

        for(String userEmail : emails) {
            Member member = memberRepository.findByMbEmail(userEmail).orElseThrow(
                    () -> new EntityNotFoundException("회원이 존재하지 않습니다."));

            Optional<ChatParticipant> chatParticipant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);

            // 참여하려고 하는 멤버가 참여자가 아닌 경우 새로 등록해주는 로직
            if (!chatParticipant.isPresent()) {
                addParticipant(chatRoom, member);

                // --------- 채팅 목록 새로고침 신호 발송 -----------
                // 간단한 JSON 객체 형태의 신호 생성
                Map<String, String> reloadSignal = new HashMap<>();
                reloadSignal.put("action", "reloadChatList"); // 클라이언트가 식별할 수 있는 액션 이름
                reloadSignal.put("invitedUser", userEmail); // **핵심: 초대받은 사용자 이메일 포함**

                // 초대받은 사용자에게만 신호 발송
                messageTemplate.convertAndSend(
                        "/topic/invitations",
                        reloadSignal        // 새로고침 신호 메시지
                );
                // ---------------------------------------------
            }
        }

        // 초대 알림 메시지 생성 및 발송
        String inviteMessageContent = String.join(", ", emails) + " 님을 채팅방에 초대했습니다."; // 또는 member.getName() 사용

        ChatMessageDto inviteNotification = ChatMessageDto.builder()
                .sender("system") // 또는 나간 사용자 이메일/이름
                .content(inviteMessageContent)
                .build();

        messageTemplate.convertAndSend("/topic/" + roomId, inviteNotification);

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

    @Override
    public boolean isRoomParticipant(String userEmail, Long roomId) {
//        채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->
                new EntityNotFoundException("room cannot be found"));
//        member 조회
        Member member = memberRepository.findByMbEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for(ChatParticipant c : chatParticipants) {
            if(c.getMember().equals(member)) {
                return true;
            }
        }
        return false;
    }

    // 채팅방에 저장되어 있는 메시지 불러오는 메소드
    @Override
    public List<ChatMessageDto> getHistory(String userEmail, Long roomId) {

        // 해당 방에 참여자인지 아닌지를 검증한다.
        boolean check = isRoomParticipant(userEmail, roomId);
        if (!check) {
            throw new EntityNotFoundException("room cannot be found");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->
                new EntityNotFoundException("room cannot be found"));

        Member member = memberRepository.findByMbEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        ChatParticipant chatParticipant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new EntityNotFoundException("participant cannot be found."));

        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(chatRoom, chatParticipant.getCreatedAt());

        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            ChatMessageDto dto = ChatMessageDto.builder()
                    .sender(chatMessage.getMember().getMbEmail())
                    .content(chatMessage.getContent())
                    .createdAt(chatMessage.getCreatedAt())
                    .build();
            chatMessageDtos.add(dto);
        }

        return chatMessageDtos;
    }

    @Override
    public void messageRead(Long roomId, String userEmail) {
//        이 api는 어떤 사람이 특정 룸의 메시지를 모두 읽었다라고 처리해주는 api라고 생각하면 된다.

//        채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->
                new EntityNotFoundException("room cannot be found"));
//        member 조회
        Member member = memberRepository.findByMbEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        List<MessageReadStatus> readStatuses = readStatusRepository.findByChatRoomAndMember(chatRoom, member);
        for(MessageReadStatus r : readStatuses) {
            r.updateIsRead(true);
        }
    }

    @Override
    public void leaveChatRoom(Long roomId, String userEmail) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->
                new EntityNotFoundException("room cannot be found"));

        Member member = memberRepository.findByMbEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        ChatParticipant participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member)
                .orElseThrow(() -> new EntityNotFoundException("participant cannot be found."));

        chatParticipantRepository.delete(participant);

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);

        // 퇴장 알림 메시지 생성 및 발송
        String leaveMessageContent = userEmail + " 님이 채팅방을 나갔습니다."; // 또는 member.getName() 사용
        ChatMessageDto leaveNotification = ChatMessageDto.builder()
                .sender("system") // 또는 나간 사용자 이메일/이름
                .content(leaveMessageContent)
                .build();

        messageTemplate.convertAndSend("/topic/" + roomId, leaveNotification);

        if(chatParticipants.isEmpty()) {
            chatRoomRepository.delete(chatRoom);
        }
    }
}
