package me.mmtr.spingbootwebsockets.model;


public class Response {
    private final String content;

    public Response(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
