package me.mmtr.spingbootwebsockets;

import me.mmtr.spingbootwebsockets.model.Response;
import me.mmtr.spingbootwebsockets.model.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationControllerTests {

    @LocalServerPort
    private int port;
    private String URL;

    private WebSocketStompClient stompClient;

    private BlockingQueue<Response> responseQueue;

    private final StompFrameHandler stompFrameHandler = new StompFrameHandler() {
        @Override
        public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
            return WebSocketMessage.class;
        }

        @Override
        public void handleFrame(@NonNull StompHeaders headers, Object payload) {
            WebSocketMessage message = (WebSocketMessage) payload;

            Response response = new Response(message.getContent());
            responseQueue.offer(response);
        }
    };

    @BeforeEach
    public void setUp() {
        URL = "ws://localhost:" + port + "/ws";

        responseQueue = new LinkedBlockingQueue<>();
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void shouldSuccessfullyReceiveMessageOnGivenTopic() throws Exception {
        StompSession session = stompClient.connectAsync(URL, new StompSessionHandlerAdapter() {
        }).get(3, TimeUnit.SECONDS);

        session.subscribe("/topic/responses", stompFrameHandler);

        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setContent("Message Content");

        session.send("/app/messages", webSocketMessage);

        Response response = responseQueue.poll(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals("Received message: " + "Message Content", response.getContent());
    }
}
