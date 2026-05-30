package com.jushi.demo.main.utils;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;

public class BackgroundNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "Notification listener connected");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w(TAG, "Notification listener disconnected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        MessageModel message = NotificationMessageParser.parse(sbn);
        if (message == null) {
            return;
        }

        if (!shouldAccept(message)) {
            return;
        }

        FilteredNotificationJsonStore.append(getApplicationContext(), message);
        Log.i(TAG, "Captured and stored filtered notification");
    }

    private boolean shouldAccept(MessageModel message) {
        List<NotificationRuleStore.Rule> rules = NotificationRuleStore.loadRules(getApplicationContext());
        if (rules.isEmpty()) {
            return true;
        }

        for (NotificationRuleStore.Rule rule : rules) {
            boolean packageMatched = rule.packageName.isEmpty()
                    || message.getSessionId().startsWith(rule.packageName + "_");
            String groupName = extractGroupNameFromSessionId(message.getSessionId());
            boolean groupMatched = rule.groupName.isEmpty()
                    || groupName.contains(rule.groupName);
            if (packageMatched && groupMatched) {
                return true;
            }
        }
        return false;
    }

    private String extractGroupNameFromSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return "";
        }
        int index = sessionId.indexOf('_');
        if (index < 0 || index + 1 >= sessionId.length()) {
            return "";
        }
        return sessionId.substring(index + 1);
    }
}
