package com.jushi.demo.main.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class NotificationTextFileStore {
    private static final String TAG = "NotificationFile";
    private static final String DIR_NAME = "Macrosoft";
    private static final String FILE_NAME = "log.txt";
    private static final String RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/" + DIR_NAME;

    private NotificationTextFileStore() {
    }

    public static synchronized void appendMessage(Context context, MessageModel message) {
        if (context == null || message == null) {
            return;
        }

        String formatted = formatMessage(message);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appendWithMediaStore(context, formatted);
            } else {
                appendWithLegacyFile(formatted);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to append message file: " + e.getMessage());
        }
    }

    public static synchronized String getLogFilePath(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return "/sdcard/Download/" + DIR_NAME + "/" + FILE_NAME;
        }
        File file = getLegacyLogFile();
        return file.getAbsolutePath();
    }

    private static void appendWithMediaStore(Context context, String text) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = findExistingDownloadUri(resolver);
        if (uri == null) {
            uri = createDownloadUri(resolver);
        }
        if (uri == null) {
            throw new IOException("Unable to create MediaStore uri");
        }

        try (OutputStream os = resolver.openOutputStream(uri, "wa")) {
            if (os == null) {
                throw new IOException("Output stream is null");
            }
            os.write(text.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private static Uri findExistingDownloadUri(ContentResolver resolver) {
        String[] projection = new String[]{MediaStore.Downloads._ID};
        String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND "
                + MediaStore.Downloads.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{FILE_NAME, RELATIVE_PATH + "/"};

        try (Cursor cursor = resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
            }
        }
        return null;
    }

    private static Uri createDownloadUri(ContentResolver resolver) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH + "/");
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    }

    private static void appendWithLegacyFile(String text) throws IOException {
        File file = getLegacyLogFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory");
        }

        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        }
    }

    private static File getLegacyLogFile() {
        File root = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), DIR_NAME);
        return new File(root, FILE_NAME);
    }

    private static String formatMessage(MessageModel message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Time: ").append(formatTime(message.getTimestamp())).append('\n');
        sb.append("Package: ").append(safe(message.getPackageName())).append('\n');
        sb.append("Title: ").append(safe(message.getTitle())).append('\n');
        sb.append("Content: ").append(safe(message.getContent())).append('\n');

        if (!TextUtils.isEmpty(message.getGroupName())) {
            sb.append("Group: ").append(message.getGroupName()).append('\n');
        }
        if (!TextUtils.isEmpty(message.getSenderName())) {
            sb.append("Sender: ").append(message.getSenderName()).append('\n');
        }

        sb.append("NotificationId: ").append(message.getNotificationId()).append('\n');
        sb.append("----------------------------------------\n");
        return sb.toString();
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0L) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(timestamp));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
