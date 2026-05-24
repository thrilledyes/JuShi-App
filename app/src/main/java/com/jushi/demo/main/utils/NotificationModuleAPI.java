package com.jushi.demo.main.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class NotificationModuleAPI {
    private static final String TAG = "NotificationModule";
    private final NotificationCaptureService captureService;
    private final Context context;

    public NotificationModuleAPI(Context context) {
        this.context = context.getApplicationContext();
        this.captureService = new NotificationCaptureService(this.context);
        Log.d(TAG, "Notification module initialized");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String captureLatestMessageAsString() {
        try {
            MessageModel message = captureService.captureLatestMessage();
            if (message != null) {
                NotificationCaptureService.printMessageLog(message);
                return message.toString();
            }
            return "No message captured";
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture latest message: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String captureAllMessagesAsString() {
        try {
            List<MessageModel> messages = captureService.captureAllMessages();
            if (messages.isEmpty()) {
                return "No message captured";
            }
            NotificationCaptureService.printMessagesLog(messages);

            StringBuilder result = new StringBuilder();
            result.append("[Captured Messages] total=").append(messages.size()).append("\n");
            for (int i = 0; i < messages.size(); i++) {
                result.append("--- #").append(i + 1).append(" ---\n");
                result.append(messages.get(i).toString()).append("\n\n");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture all messages: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String filterLatestMessageByRule(String targetPackageName, String targetGroupName) {
        try {
            NotificationFilter.CustomRuleFilter filter =
                    new NotificationFilter.CustomRuleFilter(targetPackageName, targetGroupName);
            captureService.setMessageFilter(filter);

            MessageModel message = captureService.captureAndFilterLatest();
            if (message != null) {
                NotificationCaptureService.printMessageLog(message);
                return message.toString();
            }

            return "No matching message found (package=" + targetPackageName
                    + ", group=" + targetGroupName + ")";
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter latest message: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String filterAllMessagesByRule(String targetPackageName, String targetGroupName) {
        try {
            NotificationFilter.CustomRuleFilter filter =
                    new NotificationFilter.CustomRuleFilter(targetPackageName, targetGroupName);
            captureService.setMessageFilter(filter);

            List<MessageModel> messages = captureService.captureAndFilterAll();
            if (messages.isEmpty()) {
                return "No matching messages found";
            }

            NotificationCaptureService.printMessagesLog(messages);
            StringBuilder result = new StringBuilder();
            result.append("[Filtered Messages] total=").append(messages.size()).append("\n");
            for (int i = 0; i < messages.size(); i++) {
                result.append("--- #").append(i + 1).append(" ---\n");
                result.append(messages.get(i).toString()).append("\n\n");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter all messages: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String filterWeChatMessage(String groupName) {
        try {
            NotificationFilter.WeChatFilter filter = new NotificationFilter.WeChatFilter(groupName);
            captureService.setMessageFilter(filter);

            MessageModel message = captureService.captureAndFilterLatest();
            if (message != null) {
                NotificationCaptureService.printMessageLog(message);
                return message.toString();
            }

            return "No WeChat message captured";
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter WeChat message: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String filterAICourseGroupMessage() {
        try {
            NotificationFilter.AICourseGroupFilter filter = new NotificationFilter.AICourseGroupFilter();
            captureService.setMessageFilter(filter);

            MessageModel message = captureService.captureAndFilterLatest();
            if (message != null) {
                NotificationCaptureService.printMessageLog(message);
                return message.toString();
            }

            return "No AI course message captured";
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter AI course message: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public String filterWeChatAICourseMessage() {
        try {
            List<NotificationFilter.IMessageFilter> filters = new ArrayList<>();
            filters.add(new NotificationFilter.WeChatFilter(null));
            filters.add(new NotificationFilter.AICourseGroupFilter());

            NotificationFilter.CompositeAndFilter compositeFilter =
                    new NotificationFilter.CompositeAndFilter(filters);
            captureService.setMessageFilter(compositeFilter);

            MessageModel message = captureService.captureAndFilterLatest();
            if (message != null) {
                NotificationCaptureService.printMessageLog(message);
                return message.toString();
            }

            return "No WeChat AI course message captured";
        } catch (Exception e) {
            Log.e(TAG, "Failed to filter WeChat AI course message: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public MessageModel captureLatestMessage() {
        return captureService.captureLatestMessage();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public List<MessageModel> captureAllMessages() {
        return captureService.captureAllMessages();
    }

    public void setCustomFilter(NotificationFilter.IMessageFilter filter) {
        captureService.setMessageFilter(filter);
    }

    public boolean isBackgroundCaptureEnabled() {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(context.getPackageName());
    }

    public void clearCapturedMessages() {
        NotificationMessageStore.clear(context);
    }

    public String getCapturedMessagesFilePath() {
        return NotificationTextFileStore.getLogFilePath(context);
    }

    public static void printModuleInfo() {
        Log.i(TAG, "Notification module ready");
    }
}
