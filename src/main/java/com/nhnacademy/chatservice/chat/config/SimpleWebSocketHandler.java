//package com.nhnacademy.chatservice.chat.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@Component
//public class SimpleWebSocketHandler extends TextWebSocketHandler {
//
//    // 연결된 세션 관리
//    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        // 연결이 된 후에 이루어질 작업을 정의할 메서드
//        sessions.add(session);
//        log.info("Connected: " + session.getId());
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//        log.info("Received: " + payload);
//        for (WebSocketSession otherSession : sessions) {
//            // 연결된 세션들에게 받은 메세지 전달
//            otherSession.sendMessage(new TextMessage(payload));
//        }
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        sessions.remove(session);
//        log.info("Disconnected: " + session.getId());
//    }
//}
