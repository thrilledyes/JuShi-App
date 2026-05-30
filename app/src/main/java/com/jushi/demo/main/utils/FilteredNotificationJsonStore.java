package com.jushi.demo.main.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class FilteredNotificationJsonStore {
    private static final String OUTPUT_FILE_NAME = "filtered_notifications.json";
    private static final int MAX_MESSAGES = 500;

    private FilteredNotificationJsonStore() {
    }

    public static synchronized void append(Context context, MessageModel message) {
        if (context == null || message == null) {
            return;
        }

        JSONArray messages = readMessages(context);
        messages.put(message.toJson());

        if (messages.length() > MAX_MESSAGES) {
            JSONArray trimmed = new JSONArray();
            int start = messages.length() - MAX_MESSAGES;
            for (int i = start; i < messages.length(); i++) {
                trimmed.put(messages.opt(i));
            }
            messages = trimmed;
        }

        try {
            JSONObject root = new JSONObject();
            root.put("messages", messages);
            writeRoot(context, root);
        } catch (JSONException ignored) {
        }
    }

    private static JSONArray readMessages(Context context) {
        File file = new File(context.getFilesDir(), OUTPUT_FILE_NAME);
        if (!file.exists()) {
            return new JSONArray();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = fis.read(bytes);
            if (read <= 0) {
                return new JSONArray();
            }
            String raw = new String(bytes, 0, read, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(raw);
            JSONArray arr = root.optJSONArray("messages");
            return arr == null ? new JSONArray() : arr;
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static void writeRoot(Context context, JSONObject root) {
        File file = new File(context.getFilesDir(), OUTPUT_FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception ignored) {
        }
    }
}
