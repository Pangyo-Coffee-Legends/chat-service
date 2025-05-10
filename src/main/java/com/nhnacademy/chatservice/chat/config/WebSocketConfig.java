//package com.nhnacademy.chatservice.chat.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.config.annotation.EnableWebSocket;
//import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
//import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
//
//@Configuration
//@EnableWebSocket
//public class WebSocketConfig implements WebSocketConfigurer {
//
//    private final SimpleWebSocketHandler simpleWebSocketHandler;
//
//    public WebSocketConfig(SimpleWebSocketHandler simpleWebSocketHandler) {
//        this.simpleWebSocketHandler = simpleWebSocketHandler;
//    }
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        // connect url로 webSocket 요청이 들어오면, 핸들러 클래스가 처리해준다.
//        registry.addHandler(simpleWebSocketHandler, "/api/v1/chat/connect")
//                // securityConfig에서의 cors 예외는 http 요청에 대한 예외이기 때문에
//                // webSocket 프로토콜 요청에 대해서는 별도의 cors 설정이 필요
//                .setAllowedOrigins("*");
//    }
//}
