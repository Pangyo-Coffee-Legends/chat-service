package com.nhnacademy.chatservice.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // endpoint url로 webSocket 요청이 들어오면, STOMP에서 클라이언트별, topic(=room)별로 구독정보 저장
        // endpoint는 최초 연결을 맺기 위한 url이다.
        registry.addEndpoint("ws/chat/connect")
                .setAllowedOriginPatterns("http://localhost:10253")
        // sockJs 라이브러리를 통한 요청을 허용하는 설정
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 보낼 때 사용하는 목적지 prefix
        // 클라이언트에서 /publish 형태로 시작하는 url 패턴으로 메시지가 발행되면 @Controller 객체의 @MessageMapping 메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/publish");
        // 서버가 메시지를 클라이언트에게 발행할 때 사용하는 경로 prefix
        registry.enableSimpleBroker("/topic");

//        registry.setUserDestinationPrefix("/user");
    }
}
