package com.jushi.demo.main.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class NotificationMessageStore {
    private static final String TAG = "NotificationStore";
    private static final String PREFS_NAME = "notification_message_store";
    private static final String KEY_MESSAGES = "messages";
    private static final int MAX_MESSAGES = 100;

    private NotificationMessageStore() {
    }

    public static synchronized void appendMessage(Context context, MessageModel message) {
        if (context == null || message == null) {
            return;
        }

        List<MessageModel> messages = getMessages(context);
        messages.add(0, message);
        if (messages.size() > MAX_MESSAGES) {
            messages = new ArrayList<>(messages.subList(0, MAX_MESSAGES));
        }
        saveMessages(context, messages);
        NotificationTextFileStore.appendMessage(context, message);
        Log.d(TAG, "Stored notification message: " + message.toSimpleString());
    }

    public static synchronized MessageModel getLatestMessage(Context context) {
        List<MessageModel> messages = getMessages(context);
        return messages.isEmpty() ? null : messages.get(0);
    }

    public static synchronized List<MessageModel> getMessages(Context context) {
        List<MessageModel> result = new ArrayList<>();
        if (context == null) {
            return result;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MESSAGES, "[]");
        if (TextUtils.isEmpty(raw)) {
            return result;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                MessageModel message = MessageModel.fromJson(object);
                if (message != null) {
                    result.add(message);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse cached messages: " + e.getMessage());
        }

        return result;
    }

    public static synchronized void clear(Context context) {
        if (context == null) {
            return;
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_MESSAGES)
                .apply();
    }

    private static void saveMessages(Context context, List<MessageModel> messages) {
        JSONArray array = new JSONArray();
        for (MessageModel message : messages) {
            array.put(message.toJson());
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MESSAGES, array.toString())
                .apply();
    }
}
