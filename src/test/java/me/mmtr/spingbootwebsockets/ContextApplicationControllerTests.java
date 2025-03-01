package me.mmtr.spingbootwebsockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mmtr.spingbootwebsockets.model.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ContextApplicationControllerTests.TestConfig.class,
        ContextApplicationControllerTests.TestWebSocketConfig.class})
public class ContextApplicationControllerTests {

    @Autowired
    private AbstractSubscribableChannel clientOutboundChannel;

    private TestChannelInterceptor clientOutboundChannelInterceptor;


    @BeforeEach
    public void setup() {
        this.clientOutboundChannelInterceptor = new TestChannelInterceptor();

        this.clientOutboundChannel.addInterceptor(this.clientOutboundChannelInterceptor);
    }

    @Test
    public void sendMessageTest() throws Exception {
        Message message = new Message();
        message.setContent("Message Content");

        byte[] payload = new ObjectMapper().writeValueAsBytes(message);

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        headerAccessor.setDestination("/topic/messages");
        headerAccessor.setSessionId("0");
        headerAccessor.setSessionAttributes(new HashMap<>());

        org.springframework.messaging.Message<byte[]> messageToSend = MessageBuilder
                .createMessage(payload, headerAccessor.getMessageHeaders());

        this.clientOutboundChannel.send(messageToSend);

        org.springframework.messaging.Message<?> positionUpdate = this.clientOutboundChannelInterceptor
                .awaitMessage(5);
        Assertions.assertNotNull(positionUpdate);

        StompHeaderAccessor positionUpdateHeaders = StompHeaderAccessor.wrap(positionUpdate);
        Assertions.assertEquals("/topic/messages", positionUpdateHeaders.getDestination());
        Assertions.assertEquals("0", positionUpdateHeaders.getSessionId());

        String json = new String((byte[]) positionUpdate.getPayload(), StandardCharsets.UTF_8);
        new JsonPathExpectationsHelper("$.content")
                .assertValue(json, "Message Content");

    }

    @Configuration
    @EnableWebSocketMessageBroker
    static class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker("/topic");
            registry.setApplicationDestinationPrefixes("/app");
        }

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/ws");
        }
    }

    @Configuration
    static class TestConfig implements ApplicationListener<ContextRefreshedEvent> {

        @Autowired
        private List<SubscribableChannel> channels;

        @Autowired
        private List<MessageHandler> handlers;

        @Override
        public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
            for (MessageHandler handler : handlers) {
                if (handler instanceof SimpAnnotationMethodMessageHandler) {
                    continue;
                }
                for (SubscribableChannel channel : channels) {
                    channel.unsubscribe(handler);
                }
            }
        }
    }
}
