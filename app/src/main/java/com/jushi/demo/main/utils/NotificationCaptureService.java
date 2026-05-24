package com.jushi.demo.main.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCaptureService {
    private static final String TAG = "NotificationCapture";
    private final Context context;
    private NotificationFilter.IMessageFilter messageFilter;

    public NotificationCaptureService(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setMessageFilter(NotificationFilter.IMessageFilter filter) {
        this.messageFilter = filter;
        Log.d(TAG, "Message filter updated");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MessageModel captureLatestMessage() {
        try {
            MessageModel cachedMessage = NotificationMessageStore.getLatestMessage(context);
            if (cachedMessage != null) {
                Log.i(TAG, "Using cached background notification: " + cachedMessage.toSimpleString());
                return cachedMessage;
            }

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager unavailable");
                return null;
            }

            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            if (notifications == null || notifications.length == 0) {
                Log.d(TAG, "No active notifications available");
                return null;
            }

            StatusBarNotification latestNotification = notifications[notifications.length - 1];
            MessageModel message = extractMessageFromNotification(latestNotification);
            if (message != null) {
                NotificationMessageStore.appendMessage(context, message);
                Log.i(TAG, "Captured active notification: " + message.toSimpleString());
            }
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture latest message: " + e.getMessage());
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public List<MessageModel> captureAllMessages() {
        List<MessageModel> cachedMessages = NotificationMessageStore.getMessages(context);
        if (!cachedMessages.isEmpty()) {
            Log.i(TAG, "Using " + cachedMessages.size() + " cached background notifications");
            return cachedMessages;
        }

        List<MessageModel> messageList = new ArrayList<>();
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager unavailable");
                return messageList;
            }

            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            if (notifications == null || notifications.length == 0) {
                Log.d(TAG, "No active notifications available");
                return messageList;
            }

            for (StatusBarNotification notification : notifications) {
                MessageModel message = extractMessageFromNotification(notification);
                if (message != null) {
                    messageList.add(message);
                    NotificationMessageStore.appendMessage(context, message);
                }
            }

            Log.i(TAG, "Captured " + messageList.size() + " active notifications");
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture all messages: " + e.getMessage());
        }

        return messageList;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MessageModel captureAndFilterLatest() {
        if (messageFilter == null) {
            return captureLatestMessage();
        }

        MessageModel latest = captureLatestMessage();
        if (latest == null) {
            return null;
        }

        return messageFilter.match(latest) ? latest : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public List<MessageModel> captureAndFilterAll() {
        List<MessageModel> allMessages = captureAllMessages();
        if (messageFilter == null) {
            return allMessages;
        }

        List<MessageModel> filteredMessages = new ArrayList<>();
        for (MessageModel message : allMessages) {
            if (messageFilter.match(message)) {
                filteredMessages.add(message);
            }
        }

        Log.i(TAG, "Filter result: " + allMessages.size() + " -> " + filteredMessages.size());
        return filteredMessages;
    }

    public static MessageModel extractMessageFromNotification(StatusBarNotification notification) {
        if (notification == null) {
            return null;
        }

        try {
            MessageModel message = new MessageModel();
            message.setPackageName(notification.getPackageName());
            message.setNotificationId(notification.getId());
            message.setTimestamp(notification.getPostTime());

            Notification notif = notification.getNotification();
            if (notif == null || notif.extras == null) {
                return null;
            }

            String title = notif.extras.getString(Notification.EXTRA_TITLE, "");
            String text = notif.extras.getString(Notification.EXTRA_TEXT, "");
            CharSequence bigText = notif.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

            message.setTitle(title);
            message.setContent(bigText != null ? bigText.toString() : text);

            if (notification.getPackageName() != null
                    && notification.getPackageName().contains("com.tencent.mm")) {
                String groupName = extractGroupNameFromWechatNotification(title);
                if (groupName != null) {
                    message.setGroupName(groupName);
                }
            }

            return message;
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract notification: " + e.getMessage());
            return null;
        }
    }

    private static String extractGroupNameFromWechatNotification(String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }

        String groupName = title.replaceAll("\\(\\d+\\)$", "").trim();
        return groupName.isEmpty() ? null : groupName;
    }

    public static void printMessageLog(MessageModel message) {
        if (message == null) {
            Log.i(TAG, "Message is null");
            return;
        }

        StringBuilder log = new StringBuilder();
        log.append("\n========== Captured Message ==========\n");
        log.append("Package: ").append(message.getPackageName()).append("\n");
        log.append("Title: ").append(message.getTitle()).append("\n");
        log.append("Content: ").append(message.getContent()).append("\n");
        if (message.getGroupName() != null && !message.getGroupName().isEmpty()) {
            log.append("Group: ").append(message.getGroupName()).append("\n");
        }
        log.append("Time: ").append(formatTime(message.getTimestamp())).append("\n");
        log.append("================================\n");
        Log.i(TAG, log.toString());
    }

    public static void printMessagesLog(List<MessageModel> messages) {
        if (messages == null || messages.isEmpty()) {
            Log.i(TAG, "Message list is empty");
            return;
        }

        StringBuilder log = new StringBuilder();
        log.append("\n========== Message List (").append(messages.size()).append(") ==========\n");
        for (int i = 0; i < messages.size(); i++) {
            MessageModel msg = messages.get(i);
            log.append("--- #").append(i + 1).append(" ---\n");
            log.append("Package: ").append(msg.getPackageName()).append("\n");
            log.append("Title: ").append(msg.getTitle()).append("\n");
            log.append("Content: ").append(msg.getContent()).append("\n");
            if (msg.getGroupName() != null && !msg.getGroupName().isEmpty()) {
                log.append("Group: ").append(msg.getGroupName()).append("\n");
            }
        }
        log.append("========================================\n");
        Log.i(TAG, log.toString());
    }

    private static String formatTime(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
