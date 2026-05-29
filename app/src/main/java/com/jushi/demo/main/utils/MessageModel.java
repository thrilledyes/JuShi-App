package com.jushi.demo.main.utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 通知消息数据模型
 * 用于存储从系统通知栏抓取的消息信息
 */
public class MessageModel {
    private String packageName;      // 应用包名
    private String appName;          // 应用名称
    private String title;            // 消息标题
    private String content;          // 消息内容
    private long timestamp;          // 消息时间戳
    private String senderName;       // 发送者名称（适用于聊天应用）
    private String groupName;        // 群组名称（适用于群聊）
    private int notificationId;      // 系统通知ID

    public MessageModel() {
    }

    public MessageModel(String packageName, String title, String content) {
        this.packageName = packageName;
        this.title = title;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * 转换为格式化字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("【消息信息】\n");
        sb.append("应用包名: ").append(packageName).append("\n");
        if (appName != null && !appName.isEmpty()) {
            sb.append("应用名称: ").append(appName).append("\n");
        }
        sb.append("消息标题: ").append(title).append("\n");
        sb.append("消息内容: ").append(content).append("\n");
        if (groupName != null && !groupName.isEmpty()) {
            sb.append("群组名称: ").append(groupName).append("\n");
        }
        if (senderName != null && !senderName.isEmpty()) {
            sb.append("发送者: ").append(senderName).append("\n");
        }
        sb.append("时间戳: ").append(timestamp);
        return sb.toString();
    }

    /**
     * 转换为简洁字符串
     */
    public String toSimpleString() {
        return String.format("[%s] %s - %s", packageName, title, content);
    }

    /**
     * 转换为JSON字符串（用于日志记录）
     */
    public String toJsonString() {
        return String.format("{\"package\":\"%s\",\"title\":\"%s\",\"content\":\"%s\",\"group\":\"%s\",\"timestamp\":%d}",
                packageName, title, content, groupName != null ? groupName : "", timestamp);
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("packageName", packageName);
            object.put("appName", appName);
            object.put("title", title);
            object.put("content", content);
            object.put("timestamp", timestamp);
            object.put("senderName", senderName);
            object.put("groupName", groupName);
            object.put("notificationId", notificationId);
        } catch (JSONException ignored) {
        }
        return object;
    }

    public static MessageModel fromJson(JSONObject object) {
        if (object == null) {
            return null;
        }

        MessageModel message = new MessageModel();
        message.setPackageName(object.optString("packageName", ""));
        message.setAppName(object.optString("appName", ""));
        message.setTitle(object.optString("title", ""));
        message.setContent(object.optString("content", ""));
        message.setTimestamp(object.optLong("timestamp", 0L));
        message.setSenderName(object.optString("senderName", ""));
        message.setGroupName(object.optString("groupName", ""));
        message.setNotificationId(object.optInt("notificationId", 0));
        return message;
    }
}
