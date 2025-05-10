package com.nhnacademy.chatservice.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 스프링과 STOMP는 기본적으로 세션관리를 자동(내부적)으로 처리한다.
// 연결/해제 이벤트를 기록, 연결된 세션수를 실시간으로 확인하는 목적으로 이벤트 리스너를 생성 -> 로그, 디버깅 목적
@Slf4j
@Component
public class stompEventListener {
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void connectHandle(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.add(headerAccessor.getSessionId());
        log.info("Connected : " + headerAccessor.getSessionId());
        log.info("total sessions : " + sessions.size());
    }

    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.remove(headerAccessor.getSessionId());
        log.info("disConnected : " + headerAccessor.getSessionId());
        log.info("total sessions : " + sessions.size());
    }
}
