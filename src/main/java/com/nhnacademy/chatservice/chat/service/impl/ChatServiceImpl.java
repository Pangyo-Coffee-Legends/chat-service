package com.nhnacademy.chatservice.chat.service.impl;

import com.nhnacademy.chatservice.chat.config.ChatSessionTracker;
import com.nhnacademy.chatservice.chat.domain.*;
import com.nhnacademy.chatservice.chat.dto.*;
import com.nhnacademy.chatservice.chat.repository.*;
import com.nhnacademy.chatservice.chat.service.ChatService;
import com.nhnacademy.chatservice.member.domain.Member;
import com.nhnacademy.chatservice.member.domain.Role;
import com.nhnacademy.chatservice.member.dto.MemberDto;
import com.nhnacademy.chatservice.member.repository.MemberRepository;
import com.nhnacademy.chatservice.member.repository.RoleRepository;
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
    private final RoleRepository roleRepository;
    private final NotificationMessageRepository notificationMessageRepository;

    private final ChatSessionTracker chatSessionTracker;


    public ChatServiceImpl(SimpMessageSendingOperations messageTemplate, ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, MessageReadStatusRepository readStatusRepository, MemberRepository memberRepository, RoleRepository roleRepository, NotificationMessageRepository notificationMessageRepository, ChatSessionTracker chatSessionTracker) {
        this.messageTemplate = messageTemplate;
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.notificationMessageRepository = notificationMessageRepository;
        this.chatSessionTracker = chatSessionTracker;
    }

    // 메시지 저장
    @Override
    public ChatMessage saveMessage(Long roomId, ChatMessageDto chatMessageDto) {
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

        ChatMessage saved = chatMessageRepository.save(chatMessage);
//        System.out.println("saved = " + saved.toString());

        // 채팅방에 존재하는 참여자들의 메시지 읽음 테이블에 읽음 여부를 저장
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);

        for (ChatParticipant participant : chatParticipants) {
            Member member = participant.getMember();

            boolean isInRoom = chatSessionTracker.isUserInRoom(member.getMbEmail(), roomId);

            MessageReadStatus status = MessageReadStatus.builder()
                    .chatRoom(chatRoom)
                    .member(member)
                    .chatMessage(chatMessage)
                    .isRead(member.equals(sender) || isInRoom)
                    .build();

            readStatusRepository.save(status);

            if (!member.equals(sender) && !isInRoom) {
                Long unreadCount = readStatusRepository.countByMemberAndIsReadFalse(member);
                sendUnreadCountUpdate(member.getMbEmail(), unreadCount);
            }
        }

        chatSessionTracker.getChatListSessionIdToUserEmailMap().values().forEach(
                email -> updateChatList(email));

        return saved;
    }

    public void sendUnreadCountUpdate(String userEmail, Long unreadCount) {
        // DTO를 사용하는 것이 더 좋습니다.
        Map<String, Object> payload = new HashMap<>();
        payload.put("userEmail", userEmail);
        payload.put("unreadCount", unreadCount);

        // 공용 토픽으로 userEmail과 함께 안 읽은 메시지 수 전송
        messageTemplate.convertAndSend("/topic/unread-count-updates", payload);
    }

    // 내가 읽지 않은 메시지의 개수
    @Override
    public Long getUnreadCount(String userEmail) {
        Member member = memberRepository.findByMbEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        return readStatusRepository.countByMemberAndIsReadFalse(member);
    }

    // 그룹 채팅방 개설 / 참여자의 이메일은 헤더에서 받는걸로 변경
    @Override
    public Long createGroupRoom(String userEmail, String roomName) {
        Member member= memberRepository.findByMbEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .isGroupChat("Y")
                .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);

        // 참여자 생성
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);

        return saved.getId();
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

    @Override
    public void updateChatList(String email) {
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
                    .email(email)
                    .build();
            chatRoomList.add(dto);
        }

        messageTemplate.convertAndSend("/topic/chat-list-updates", chatRoomList);

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
//            // 퇴장 알림 메시지 생성 및 발송
//            String inviteMessageContent = userEmail + " 님을 채팅방에 나갔습니다."; // 또는 member.getName() 사용
//
//            ChatMessageDto inviteNotification = ChatMessageDto.builder()
//                    .sender("system") // 또는 나간 사용자 이메일/이름
//                    .content(inviteMessageContent)
//                    .build();

            addParticipant(chatRoom, member);

            chatSessionTracker.getChatListSessionIdToUserEmailMap().values().forEach(
                    email -> updateChatList(email));
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

                chatSessionTracker.getChatListSessionIdToUserEmailMap().values().forEach(
                        email -> updateChatList(email));

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

        List<ChatMessage> chatMessages = chatMessageRepository
                .findByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(chatRoom, chatParticipant.getCreatedAt());

//        // 특정 채팅방의 특정 메시지의 unreadCount 의 개수를 가져오기
//        Long unreadCount = readStatusRepository.countByChatRoomAndIsReadFalse(chatRoom);

        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();

//        for (ChatMessage chatMessage : chatMessages) {
//
//            // 특정 채팅방의 특정 메시지의 unreadCount 의 개수를 가져오기
//            Long unreadCount = readStatusRepository.countByChatRoomAndChatMessageAndIsReadFalse(chatRoom, chatMessage);
//
//            ChatMessageDto dto = ChatMessageDto.builder()
//                    .id(chatMessage.getId())
//                    .sender(chatMessage.getMember().getMbEmail())
//                    .content(chatMessage.getContent())
//                    .createdAt(chatMessage.getCreatedAt())
//                    .unreadCount(unreadCount)
//                    .build();
//
//            chatMessageDtos.add(dto);
//        }

//         1. unreadCount를 한 번에 조회
        Map<Long, Long> unreadCountMap = readStatusRepository.findUnreadCountByRoomId(roomId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number)row.get("messageId")).longValue(),
                        row -> ((Number)row.get("unreadCount")).longValue()
                ));

        for (ChatMessage chatMessage : chatMessages) {
            // 2. 메시지 리스트를 순회하며 unreadCount 매핑
            Long unreadCount = unreadCountMap.getOrDefault(chatMessage.getId(), 0L);

            ChatMessageDto dto = ChatMessageDto.builder()
                    .id(chatMessage.getId())
                    .sender(chatMessage.getMember().getMbEmail())
                    .content(chatMessage.getContent())
                    .createdAt(chatMessage.getCreatedAt())
                    .unreadCount(unreadCount)
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

        chatSessionTracker.getChatListSessionIdToUserEmailMap().values().forEach(
                email -> updateChatList(email));

        if(chatParticipants.isEmpty()) {
            chatRoomRepository.delete(chatRoom);
        }

    }

    @Override
    public void sendUnreadCountUpdate(Long roomId) {

        // 1. 채팅방의 모든 메시지 조회
        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoomId(roomId);

        List<MessageReadStatus> messageReadStatuses = readStatusRepository.findByChatRoom(
                chatRoomRepository.findById(roomId).get());

        // <메시지 ID, false 개수>를 저장할 Map
        Map<Long, Integer> unreadCountMap = new HashMap<>();
        for (ChatMessage message : allMessages) {
            unreadCountMap.put(message.getId(), 0); // 일단 0으로 초기화
        }

        for (MessageReadStatus r : messageReadStatuses) {
            Long messageId = r.getChatMessage().getId();
            if (!r.isRead()) {
                unreadCountMap.put(messageId, unreadCountMap.getOrDefault(messageId, 0) + 1);
            }
        }

        messageTemplate.convertAndSend("/topic/" + roomId + "/unread", unreadCountMap);
    }

    // 알림 메시지 API

    @Override
    public List<Member> findByRole_RoleName(String roleName) {
        List<Member> roleMembers = memberRepository.findByRole_RoleName(roleName);

        if(!roleMembers.isEmpty()) {
            return roleMembers;
        }

        return List.of();
    }

    @Override
    public Role findByRoleName(String roleName) {
        Role role = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow(() -> new EntityNotFoundException("role cannot be found."));

        return role;
    }

    @Override
    public void saveNotificationMessage(Member member, Role role ,String content) {

        NotificationMessage notificationMessage = NotificationMessage.builder()
                .member(member)
                .role(role)
                .content(content)
                .build();

        NotificationMessage savedNotificationMessage = notificationMessageRepository.save(notificationMessage);

        Long count = notificationMessageRepository.countByMemberAndIsReadFalse(member);

        if(chatSessionTracker.getChatListSessionIdToUserEmailMap().containsValue(member.getMbEmail())) {
            // 현재 접속중인 사용자에게만 notification count 및 content 메시지 보냄
            messageTemplate.convertAndSend("/topic/unread-notification-count-updates/" + member.getMbEmail(), count);
            messageTemplate.convertAndSend("/topic/" + member.getMbEmail(), content);
            messageTemplate.convertAndSend("/topic/notification-message/" + member.getMbEmail(), content);
        }
    }

    @Override
    public Long getNotificationUnreadCount(String email) {
        Member member = memberRepository.findByMbEmail(email).orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        Long count = notificationMessageRepository.countByMemberAndIsReadFalse(member);

        return count;
    }

    @Override
    public void readNotification(String email) {
        Member member = memberRepository.findByMbEmail(email).orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        List<NotificationMessage> notificationMessages = notificationMessageRepository.findByMemberAndIsReadFalse(member);

        for(NotificationMessage notificationMessage : notificationMessages) {
            System.out.println("notificationMessage = " + notificationMessage);
            notificationMessage.updateIsRead(true);
        }
    }

    @Override
    public List<NotificationMessageDto> getHistoryNotification(String email) {
        Member member = memberRepository.findByMbEmail(email).orElseThrow(() -> new EntityNotFoundException("member cannot be found."));

        List<NotificationMessage> notificationMessages = notificationMessageRepository.findByMember(member);

        List<NotificationMessageDto> notificationMessageDtos = new ArrayList<>();

        for(NotificationMessage notificationMessage : notificationMessages) {
            NotificationMessageDto notificationMessageDto = NotificationMessageDto.builder()
                    .content(notificationMessage.getContent())
                    .createdAt(notificationMessage.getCreatedAt())
                    .build();
            notificationMessageDtos.add(notificationMessageDto);
        }

        return notificationMessageDtos;
    }
}