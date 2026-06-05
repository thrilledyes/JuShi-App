package com.jushi.demo.main.database.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.model.NotificationRule;
import com.jushi.demo.main.database.model.RawMessage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private final DBHelper helper;

    public DBManager(Context context) {
        helper = new DBHelper(context.getApplicationContext());
    }

    public long insertRawMessage(RawMessage msg) {
        if (msg == null) {
            return -1L;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        String messageHash = isEmpty(msg.messageHash) ? buildMessageHash(msg) : msg.messageHash;

        ContentValues values = new ContentValues();
        values.put("session_id", safe(msg.sessionId));
        values.put("source_type", msg.source == null ? "" : msg.source.name());
        values.put("sender", safe(msg.sender));
        values.put("title", msg.title);
        values.put("content", safe(msg.content));
        values.put("raw_payload", safe(msg.rawPayload));
        values.put("timestamp", msg.timestamp);
        values.put("message_hash", messageHash);

        long id = db.insertWithOnConflict(
                DBSchema.TABLE_RAW_MESSAGE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        upsertConversation(db, msg);
        return id;
    }

    public List<RawMessage> getRecentMessages(String sessionId, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<RawMessage> list = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_RAW_MESSAGE
                        + " WHERE session_id=?"
                        + " ORDER BY timestamp DESC"
                        + " LIMIT ?",
                new String[]{sessionId, String.valueOf(limit)}
        )) {
            while (cursor.moveToNext()) {
                RawMessage msg = new RawMessage();
                msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                msg.sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id"));
                msg.sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                msg.content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                msg.rawPayload = cursor.getString(cursor.getColumnIndexOrThrow("raw_payload"));
                msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                msg.messageHash = cursor.getString(cursor.getColumnIndexOrThrow("message_hash"));
                list.add(msg);
            }
        }

        return list;
    }

    public long insertNormalizedRecord(
            String courseName,
            String taskTitle,
            String description,
            String deadline,
            double confidence
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("course_name", courseName);
        values.put("task_title", taskTitle);
        values.put("description", description);
        values.put("deadline", deadline);
        values.put("confidence", confidence);
        values.put("created_at", now);
        values.put("updated_at", now);

        return db.insert(DBSchema.TABLE_NORMALIZED_RECORD, null, values);
    }

    public void linkRawToNormalized(long rawId, long normalizedId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("raw_id", rawId);
        values.put("normalized_id", normalizedId);
        db.insertWithOnConflict(
                DBSchema.TABLE_RAW_NORMALIZED_MAP,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    public void replaceNotificationRules(List<NotificationRule> rules) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            db.delete(DBSchema.TABLE_NOTIFICATION_RULE, null, null);
            if (rules != null) {
                for (NotificationRule rule : rules) {
                    ContentValues values = new ContentValues();
                    values.put("package_name", safe(rule.packageName));
                    values.put("source_name", safe(rule.sourceName));
                    values.put("group_name", safe(rule.groupName));
                    values.put("enabled", rule.enabled ? 1 : 0);
                    values.put("created_at", rule.createdAt > 0 ? rule.createdAt : now);
                    values.put("updated_at", now);
                    db.insert(DBSchema.TABLE_NOTIFICATION_RULE, null, values);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<NotificationRule> getEnabledNotificationRules() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<NotificationRule> rules = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_NOTIFICATION_RULE
                        + " WHERE enabled=1"
                        + " ORDER BY id",
                null
        )) {
            while (cursor.moveToNext()) {
                NotificationRule rule = new NotificationRule();
                rule.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                rule.packageName = cursor.getString(cursor.getColumnIndexOrThrow("package_name"));
                rule.sourceName = cursor.getString(cursor.getColumnIndexOrThrow("source_name"));
                rule.groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name"));
                rule.enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1;
                rule.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                rule.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
                rules.add(rule);
            }
        }

        return rules;
    }

    public void deleteAllCourses() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(DBSchema.TABLE_COURSES, null, null);
    }

    public void insertCourses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Course course : courses) {
                ContentValues values = new ContentValues();
                values.put("course_name", course.getCourseName());
                values.put("teacher", course.getTeacher());
                values.put("location", course.getLocation());
                values.put("day_of_week", course.getDayOfWeek());
                values.put("start_period", course.getStartPeriod());
                values.put("end_period", course.getEndPeriod());
                values.put("week_range", course.getWeekRange());
                values.put("color", course.getColor());
                db.insert(DBSchema.TABLE_COURSES, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Course> getAllCourses() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Course> courses = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_COURSES
                        + " ORDER BY day_of_week, start_period",
                null
        )) {
            while (cursor.moveToNext()) {
                courses.add(readCourse(cursor));
            }
        }

        return courses;
    }

    public List<Course> getCoursesByDayOfWeek(int dayOfWeek) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Course> courses = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_COURSES
                        + " WHERE day_of_week=?"
                        + " ORDER BY start_period",
                new String[]{String.valueOf(dayOfWeek)}
        )) {
            while (cursor.moveToNext()) {
                courses.add(readCourse(cursor));
            }
        }

        return courses;
    }

    private void upsertConversation(SQLiteDatabase db, RawMessage msg) {
        ContentValues values = new ContentValues();
        values.put("session_id", safe(msg.sessionId));
        values.put("source_type", msg.source == null ? "" : msg.source.name());
        values.put("name", safe(msg.sessionId));
        values.put("last_active", msg.timestamp);

        db.insertWithOnConflict(
                DBSchema.TABLE_CONVERSATION,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        ContentValues updates = new ContentValues();
        updates.put("last_active", msg.timestamp);
        db.update(
                DBSchema.TABLE_CONVERSATION,
                updates,
                "session_id=?",
                new String[]{safe(msg.sessionId)}
        );
    }

    private Course readCourse(Cursor cursor) {
        Course course = new Course();
        course.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        course.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow("course_name")));
        course.setTeacher(cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
        course.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
        course.setDayOfWeek(cursor.getInt(cursor.getColumnIndexOrThrow("day_of_week")));
        course.setStartPeriod(cursor.getInt(cursor.getColumnIndexOrThrow("start_period")));
        course.setEndPeriod(cursor.getInt(cursor.getColumnIndexOrThrow("end_period")));
        course.setWeekRange(cursor.getString(cursor.getColumnIndexOrThrow("week_range")));
        course.setColor(cursor.getInt(cursor.getColumnIndexOrThrow("color")));
        return course;
    }

    private static String buildMessageHash(RawMessage msg) {
        String raw = safe(msg.sessionId) + "|"
                + (msg.source == null ? "" : msg.source.name()) + "|"
                + safe(msg.sender) + "|"
                + safe(msg.title) + "|"
                + safe(msg.content) + "|"
                + msg.timestamp;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ignored) {
            return String.valueOf(raw.hashCode());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
