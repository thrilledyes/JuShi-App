package com.jushi.demo.main.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageModel {
    private String sessionId;
    private String source;
    private String sender;
    private String title;
    private String content;
    private String rawPayload;
    private long timestamp;
    private long id;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            object.put("sessionId", sessionId == null ? "" : sessionId);
            object.put("source", source == null ? "" : source);
            object.put("sender", sender == null ? "" : sender);
            object.put("title", title == null ? JSONObject.NULL : title);
            object.put("content", content == null ? "" : content);
            object.put("rawPayload", rawPayload == null ? "" : rawPayload);
            object.put("timestamp", timestamp);
        } catch (JSONException ignored) {
        }
        return object;
    }
}
