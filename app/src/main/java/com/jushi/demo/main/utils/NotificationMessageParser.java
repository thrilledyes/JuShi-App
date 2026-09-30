package com.jushi.demo.main.utils;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import org.json.JSONObject;

public final class NotificationMessageParser {
    private NotificationMessageParser() {
    }

    public static MessageModel parse(StatusBarNotification notification) {
        if (notification == null) {
            return null;
        }

        Notification notif = notification.getNotification();
        if (notif == null || notif.extras == null) {
            return null;
        }

        MessageModel message = new MessageModel();
        String packageName = notification.getPackageName() == null ? "" : notification.getPackageName();
        String title = notif.extras.getString(Notification.EXTRA_TITLE, "");
        String text = notif.extras.getString(Notification.EXTRA_TEXT, "");
        CharSequence bigText = notif.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String groupName = extractGroupNameFromTitle(title);

        message.setId(notification.getId());
        message.setSource(mapSourceType(packageName));
        message.setSender(title == null ? "" : title);
        message.setTitle(null);
        message.setContent(bigText != null ? bigText.toString() : text);
        message.setTimestamp(notification.getPostTime());
        message.setSessionId(buildSessionId(packageName, groupName));
        message.setRawPayload(buildRawPayload(notification, packageName, title, text, groupName));
        return message;
    }

    private static String extractGroupNameFromTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }
        return title.replaceAll("\\(\\d+\\)$", "").trim();
    }

    private static String mapSourceType(String packageName) {
        if ("com.tencent.mm".equals(packageName)) {
            return "WX";
        }
        if ("com.tencent.mobileqq".equals(packageName)) {
            return "QQ";
        }
        if (packageName.contains("chaoxing")) {
            return "WEB";
        }
        if ("com.huawei.easyhpc".equals(packageName)) {
            return "EASYHPC";
        }
        return "ANDROID";
    }

    private static String buildSessionId(String packageName, String groupName) {
        String normalizedGroup = (groupName == null || groupName.isEmpty()) ? "unknown" : groupName;
        return packageName + "_" + normalizedGroup;
    }

    private static String buildRawPayload(
            StatusBarNotification notification,
            String packageName,
            String title,
            String text,
            String groupName
    ) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("packageName", packageName);
            payload.put("title", title == null ? "" : title);
            payload.put("text", text == null ? "" : text);
            payload.put("groupName", groupName == null ? "" : groupName);
            payload.put("notificationId", notification.getId());
            payload.put("postTime", notification.getPostTime());
            return payload.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
