package com.jushi.demo.main.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import com.jushi.demo.main.database.model.TodoMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class TodoCalendarExporter {
    private TodoCalendarExporter() {
    }

    public static Result exportTodos(Context context, List<TodoMessage> todos) {
        Result result = new Result();
        if (context == null || todos == null || todos.isEmpty()) {
            return result;
        }

        ContentResolver resolver = context.getContentResolver();
        long calendarId = findWritableCalendarId(resolver);
        if (calendarId <= 0) {
            result.error = "没有找到可写入的手机日历";
            return result;
        }

        for (TodoMessage todo : todos) {
            if (todo == null) {
                continue;
            }
            Date deadline = parseDeadline(todo.deadline);
            if (deadline == null) {
                result.skipped++;
                continue;
            }

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.TITLE, nonEmpty(todo.title, "待办"));
            values.put(CalendarContract.Events.DESCRIPTION, buildDescription(todo));
            values.put(CalendarContract.Events.DTSTART, deadline.getTime());
            values.put(CalendarContract.Events.DTEND, deadline.getTime() + 30L * 60L * 1000L);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED);

            try {
                Uri uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values);
                if (uri == null) {
                    result.failed++;
                } else {
                    result.inserted++;
                }
            } catch (SecurityException e) {
                result.error = "没有日历写入权限";
                result.failed++;
            } catch (Exception e) {
                result.failed++;
            }
        }
        return result;
    }

    private static long findWritableCalendarId(ContentResolver resolver) {
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        };
        String selection = CalendarContract.Calendars.VISIBLE + "=1 AND "
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">="
                + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        try (Cursor cursor = resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID));
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }

    private static String buildDescription(TodoMessage todo) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(todo.content)) {
            sb.append(todo.content.trim());
        }
        String source = nonEmpty(todo.conversationTitle, todo.conversationId);
        if (!TextUtils.isEmpty(source)) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("来源：").append(source);
        }
        return sb.toString();
    }

    private static Date parseDeadline(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static final class Result {
        public int inserted;
        public int skipped;
        public int failed;
        public String error;

        public String buildMessage() {
            if (!TextUtils.isEmpty(error) && inserted == 0) {
                return error;
            }
            return "已导入 " + inserted + " 条，跳过 " + skipped + " 条，失败 " + failed + " 条";
        }
    }
}
