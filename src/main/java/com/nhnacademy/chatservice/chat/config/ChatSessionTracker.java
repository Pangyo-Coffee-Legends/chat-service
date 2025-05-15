package com.nhnacademy.chatservice.chat.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionTracker {
    // 세션ID -> 사용자 이메일
    private final Map<String, String> sessionIdToUserEmailMap = new ConcurrentHashMap<>();

    // 사용자 이메일 -> 현재 열고 있는 채팅방 ID
    private final Map<String, Long> userActiveRoomMap = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, String userEmail, Long roomId) {
        sessionIdToUserEmailMap.put(sessionId, userEmail);
        userActiveRoomMap.put(userEmail, roomId);
    }

    public void unregisterSession(String sessionId) {
        String userEmail = sessionIdToUserEmailMap.remove(sessionId);
        if (userEmail != null) {
            userActiveRoomMap.remove(userEmail);
        }
    }

    public boolean isUserInRoom(String userEmail, Long roomId) {
        return userActiveRoomMap.getOrDefault(userEmail, -1L).equals(roomId);
    }

    public Long getUserActiveRoom(String userEmail) {
        return userActiveRoomMap.get(userEmail);
    }
}

