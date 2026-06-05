package com.jushi.demo.main.database.model;

public class RawMessage {
    public long id;
    public String sessionId;
    public SourceType source;
    public String sender;
    public String title;
    public String content;
    public String rawPayload;
    public long timestamp;
    public String messageHash;
}
