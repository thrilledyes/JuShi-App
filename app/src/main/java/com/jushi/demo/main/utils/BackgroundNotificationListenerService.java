package com.jushi.demo.main.utils;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
        MessageModel message = NotificationCaptureService.extractMessageFromNotification(sbn);
        if (message == null) {
            return;
        }

        NotificationMessageStore.appendMessage(getApplicationContext(), message);
        Log.i(TAG, "Captured background notification: " + message.toSimpleString());
    }
}
