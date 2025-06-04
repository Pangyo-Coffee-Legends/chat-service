package com.nhnacademy.chatservice.chat.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionTracker {
    private final Map<String, String> chatListSessionIdToUserEmailMap = new ConcurrentHashMap<>();

    // 세션ID -> 사용자 이메일
    private final Map<String, String> roomSessionIdToUserEmailMap = new ConcurrentHashMap<>();

    // 사용자 이메일 -> 현재 열고 있는 채팅방 ID
    private final Map<String, Long> userActiveRoomMap = new ConcurrentHashMap<>();

    public void roomRegisterSession(String sessionId, String userEmail, Long roomId) {
        roomSessionIdToUserEmailMap.put(sessionId, userEmail);
        userActiveRoomMap.put(userEmail, roomId);
    }

    public void chatListRegisterSession(String sessionId, String userEmail) {
        chatListSessionIdToUserEmailMap.put(sessionId, userEmail);
    }

    public void roomUnregisterSession(String sessionId) {
        String userEmail = roomSessionIdToUserEmailMap.remove(sessionId);
        if (userEmail != null) {
            userActiveRoomMap.remove(userEmail);
        }
    }

    public void chatListUnregisterSession(String sessionId) {
        chatListSessionIdToUserEmailMap.remove(sessionId);
    }

    public boolean isUserInRoom(String userEmail, Long roomId) {
        return userActiveRoomMap.getOrDefault(userEmail, -1L).equals(roomId);
    }

    public Long getUserActiveRoom(String userEmail) {
        return userActiveRoomMap.get(userEmail);
    }

    public Map<String, String> getChatListSessionIdToUserEmailMap() {
        return chatListSessionIdToUserEmailMap;
    }
}

