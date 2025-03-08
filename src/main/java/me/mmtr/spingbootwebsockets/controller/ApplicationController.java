package me.mmtr.spingbootwebsockets.controller;

import me.mmtr.spingbootwebsockets.model.Response;
import me.mmtr.spingbootwebsockets.model.WebSocketMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class ApplicationController {

    @MessageMapping("/messages")
    @SendTo("/topic/responses")
    public Response response(WebSocketMessage webSocketMessage) {
        return new Response("Received message: " + HtmlUtils.htmlEscape(webSocketMessage.getContent()));
    }
}
