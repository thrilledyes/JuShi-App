package com.jushi.demo.main.database.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.model.CourseReminderRule;
import com.jushi.demo.main.database.model.NotificationRule;
import com.jushi.demo.main.database.model.NormalizedRecord;
import com.jushi.demo.main.database.model.RawMessage;
import com.jushi.demo.main.database.model.SourceType;
import com.jushi.demo.main.database.model.TodoConversation;
import com.jushi.demo.main.database.model.TodoMessage;

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
                list.add(readRawMessage(cursor));
            }
        }

        return list;
    }

    public List<RawMessage> getCapturedRawMessages(int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<RawMessage> list = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_RAW_MESSAGE
                        + " ORDER BY timestamp DESC"
                        + " LIMIT ?",
                new String[]{String.valueOf(limit)}
        )) {
            while (cursor.moveToNext()) {
                list.add(readRawMessage(cursor));
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

    public long insertTodoMessage(
            String conversationId,
            String conversationTitle,
            String messageType,
            String senderType,
            String title,
            String content,
            String deadline,
            long sourceRawId
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("conversation_id", safe(conversationId));
        values.put("conversation_title", safe(conversationTitle));
        values.put("message_type", safe(messageType));
        values.put("sender_type", safe(senderType));
        values.put("title", safe(title));
        values.put("content", safe(content));
        values.put("deadline", safe(deadline));
        values.put("source_raw_id", sourceRawId);
        values.put("created_at", System.currentTimeMillis());
        values.put("pinned_at", getConversationPinnedAt(db, conversationId));
        values.put("completed_at", 0);
        values.put("reminder_at", 0);
        return db.insert(DBSchema.TABLE_TODO_MESSAGE, null, values);
    }

    public void ensureCourseAssistantMessage() {
        SQLiteDatabase db = helper.getWritableDatabase();
        long count;
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=?",
                new String[]{"course_assistant"}
        )) {
            cursor.moveToFirst();
            count = cursor.getLong(0);
        }
        if (count > 0) {
            return;
        }
        insertTodoMessage(
                "course_assistant",
                "课程表助手",
                "course_welcome",
                "bot",
                "选择课程提醒",
                "请选择需要开启提醒的课程。我会在上课前一天晚上 22:00 和上课前 20 分钟提醒你。",
                "",
                -1
        );
    }

    public List<TodoConversation> getTodoConversations() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TodoConversation> list = new ArrayList<>();
        String sql = "SELECT m.conversation_id, m.conversation_title, m.content, m.deadline, m.created_at, m.pinned_at,"
                + " (SELECT COUNT(*) FROM " + DBSchema.TABLE_TODO_MESSAGE + " u"
                + " WHERE u.conversation_id=m.conversation_id AND u.read_at=0) AS unread_count"
                + " FROM " + DBSchema.TABLE_TODO_MESSAGE + " m"
                + " INNER JOIN (SELECT conversation_id, MAX(created_at) AS last_time"
                + " FROM " + DBSchema.TABLE_TODO_MESSAGE
                + " GROUP BY conversation_id) last"
                + " ON m.conversation_id=last.conversation_id AND m.created_at=last.last_time"
                + " ORDER BY m.pinned_at DESC, m.created_at DESC";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                TodoConversation conversation = new TodoConversation();
                conversation.conversationId = cursor.getString(cursor.getColumnIndexOrThrow("conversation_id"));
                conversation.title = cursor.getString(cursor.getColumnIndexOrThrow("conversation_title"));
                conversation.lastContent = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                conversation.lastDeadline = cursor.getString(cursor.getColumnIndexOrThrow("deadline"));
                conversation.lastTime = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                conversation.unreadCount = cursor.getInt(cursor.getColumnIndexOrThrow("unread_count"));
                conversation.pinnedAt = cursor.getLong(cursor.getColumnIndexOrThrow("pinned_at"));
                list.add(conversation);
            }
        }

        return list;
    }

    public void ensureCourseAssistantTodoMessage() {
        SQLiteDatabase db = helper.getWritableDatabase();
        long count;
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=?",
                new String[]{"course_assistant"}
        )) {
            cursor.moveToFirst();
            count = cursor.getLong(0);
        }
        if (count > 0) {
            return;
        }
        insertTodoMessage(
                "course_assistant",
                "课程表助手",
                "course_welcome",
                "bot",
                "选择课程提醒",
                "请选择需要开启提醒的课程。我会在上课前一天晚上 22:00 和上课前 20 分钟提醒你。",
                "",
                -1
        );
    }

    public void ensurePersonalTodoConversation() {
        SQLiteDatabase db = helper.getWritableDatabase();
        long count;
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=?",
                new String[]{"personal_todo"}
        )) {
            cursor.moveToFirst();
            count = cursor.getLong(0);
        }
        if (count > 0) {
            return;
        }
        insertTodoMessage(
                "personal_todo",
                "我的待办",
                "personal_welcome",
                "bot",
                "我的待办",
                "点击右上角 + 可以编辑或浏览自己的待办。",
                "",
                -1
        );
    }

    public List<TodoMessage> getPersonalTodoItems() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TodoMessage> list = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=? AND message_type=?"
                        + " ORDER BY deadline ASC, created_at ASC",
                new String[]{"personal_todo", "personal_todo"}
        )) {
            while (cursor.moveToNext()) {
                list.add(readTodoMessage(cursor));
            }
        }
        return list;
    }

    public List<TodoMessage> getActionableTodoItems() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TodoMessage> list = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE message_type IN (?, ?)"
                        + " ORDER BY CASE WHEN completed_at>0 THEN 1 ELSE 0 END,"
                        + " CASE WHEN deadline='' THEN 1 ELSE 0 END,"
                        + " deadline ASC, completed_at DESC, created_at DESC",
                new String[]{"ddl", "personal_todo"}
        )) {
            while (cursor.moveToNext()) {
                list.add(readTodoMessage(cursor));
            }
        }
        return list;
    }

    public TodoMessage getTodoMessageById(long messageId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE id=?",
                new String[]{String.valueOf(messageId)}
        )) {
            if (cursor.moveToFirst()) {
                return readTodoMessage(cursor);
            }
        }
        return null;
    }

    public void pinTodoConversation(String conversationId, boolean pinned) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pinned_at", pinned ? System.currentTimeMillis() : 0);
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "conversation_id=?",
                new String[]{safe(conversationId)}
        );
    }

    public void deleteTodoConversation(String conversationId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(
                DBSchema.TABLE_TODO_MESSAGE,
                "conversation_id=?",
                new String[]{safe(conversationId)}
        );
    }

    public void markTodoConversationUnread(String conversationId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("read_at", 0);
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "conversation_id=?",
                new String[]{safe(conversationId)}
        );
    }

    public void deleteTodoMessage(long messageId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(
                DBSchema.TABLE_TODO_MESSAGE,
                "id=?",
                new String[]{String.valueOf(messageId)}
        );
    }

    public void deleteTodoMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Long messageId : messageIds) {
                if (messageId == null) {
                    continue;
                }
                db.delete(
                        DBSchema.TABLE_TODO_MESSAGE,
                        "id=?",
                        new String[]{String.valueOf(messageId)}
                );
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateTodoMessageContent(long messageId, String content) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("content", safe(content));
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "id=?",
                new String[]{String.valueOf(messageId)}
        );
    }

    public void updateTodoMessage(long messageId, String title, String content, String deadline) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", safe(title));
        values.put("content", safe(content));
        values.put("deadline", safe(deadline));
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "id=?",
                new String[]{String.valueOf(messageId)}
        );
    }

    public void setTodoCompleted(long messageId, boolean completed) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completed_at", completed ? System.currentTimeMillis() : 0);
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "id=?",
                new String[]{String.valueOf(messageId)}
        );
    }

    public void updateTodoReminder(long messageId, long reminderAt) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("reminder_at", reminderAt);
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "id=?",
                new String[]{String.valueOf(messageId)}
        );
    }

    public List<TodoMessage> getTodoMessages(String conversationId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<TodoMessage> list = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=?"
                        + " ORDER BY created_at ASC, id ASC",
                new String[]{safe(conversationId)}
        )) {
            while (cursor.moveToNext()) {
                list.add(readTodoMessage(cursor));
            }
        }
        return list;
    }

    public void markTodoConversationRead(String conversationId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("read_at", System.currentTimeMillis());
        db.update(
                DBSchema.TABLE_TODO_MESSAGE,
                values,
                "conversation_id=? AND read_at=0",
                new String[]{safe(conversationId)}
        );
    }

    public List<NormalizedRecord> getRecentNormalizedRecords(int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<NormalizedRecord> list = new ArrayList<>();

        String sql = "SELECT n.id, n.course_name, n.task_title, n.description, n.deadline,"
                + " n.confidence, n.created_at, n.updated_at,"
                + " r.session_id, r.source_type, r.sender, r.content, r.timestamp"
                + " FROM " + DBSchema.TABLE_NORMALIZED_RECORD + " n"
                + " LEFT JOIN " + DBSchema.TABLE_RAW_NORMALIZED_MAP + " m"
                + " ON n.id = m.normalized_id"
                + " LEFT JOIN " + DBSchema.TABLE_RAW_MESSAGE + " r"
                + " ON m.raw_id = r.id"
                + " ORDER BY n.created_at DESC"
                + " LIMIT ?";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                NormalizedRecord record = new NormalizedRecord();
                record.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                record.courseName = cursor.getString(cursor.getColumnIndexOrThrow("course_name"));
                record.taskTitle = cursor.getString(cursor.getColumnIndexOrThrow("task_title"));
                record.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                record.deadline = cursor.getString(cursor.getColumnIndexOrThrow("deadline"));
                record.confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence"));
                record.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                record.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
                record.sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id"));
                record.sourceType = cursor.getString(cursor.getColumnIndexOrThrow("source_type"));
                record.sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                record.rawContent = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                record.rawTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                list.add(record);
            }
        }

        return list;
    }

    public void replaceNotificationRules(List<NotificationRule> rules) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            db.delete(DBSchema.TABLE_NOTIFICATION_RULE, null, null);
            if (rules != null) {
                for (NotificationRule rule : rules) {
                    db.insert(DBSchema.TABLE_NOTIFICATION_RULE, null, buildNotificationRuleValues(rule, now, true));
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void replaceNotificationRulesForPackages(String[] packageNames, List<NotificationRule> rules) {
        if (packageNames == null || packageNames.length == 0) {
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            for (String packageName : packageNames) {
                db.delete(
                        DBSchema.TABLE_NOTIFICATION_RULE,
                        "package_name=?",
                        new String[]{safe(packageName)}
                );
            }

            if (rules != null) {
                for (NotificationRule rule : rules) {
                    db.insert(DBSchema.TABLE_NOTIFICATION_RULE, null, buildNotificationRuleValues(rule, now, true));
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void upsertNotificationRules(List<NotificationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            for (NotificationRule rule : rules) {
                ContentValues values = buildNotificationRuleValues(rule, now, false);

                int updated = db.update(
                        DBSchema.TABLE_NOTIFICATION_RULE,
                        values,
                        "package_name=? AND group_name=?",
                        new String[]{safe(rule.packageName), safe(rule.groupName)}
                );

                if (updated == 0) {
                    values.put("created_at", rule.createdAt > 0 ? rule.createdAt : now);
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
                "SELECT r.*, COALESCE(NULLIF(r.course_name, ''), c.course_name, '') AS bound_course_name"
                        + " FROM " + DBSchema.TABLE_NOTIFICATION_RULE + " r"
                        + " LEFT JOIN " + DBSchema.TABLE_COURSES + " c"
                        + " ON r.course_id=c.id"
                        + " WHERE r.enabled=1"
                        + " ORDER BY r.id",
                null
        )) {
            while (cursor.moveToNext()) {
                rules.add(readNotificationRule(cursor));
            }
        }

        return rules;
    }

    public void updateNotificationRuleCourse(long id, int courseId, String courseName) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("course_id", courseId);
        values.put("course_name", safe(courseName));
        values.put("updated_at", System.currentTimeMillis());
        db.update(
                DBSchema.TABLE_NOTIFICATION_RULE,
                values,
                "id=?",
                new String[]{String.valueOf(id)}
        );
    }

    public void deleteNotificationRule(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(
                DBSchema.TABLE_NOTIFICATION_RULE,
                "id=?",
                new String[]{String.valueOf(id)}
        );
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

    public void replaceCourseReminderRules(List<CourseReminderRule> rules) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            db.delete(DBSchema.TABLE_COURSE_REMINDER_RULE, null, null);
            if (rules != null) {
                for (CourseReminderRule rule : rules) {
                    ContentValues values = new ContentValues();
                    values.put("course_id", rule.courseId);
                    values.put("enabled", rule.enabled ? 1 : 0);
                    values.put("remind_day_before", rule.remindDayBefore ? 1 : 0);
                    values.put("remind_before_minutes", rule.remindBeforeMinutes);
                    values.put("updated_at", now);
                    db.insertWithOnConflict(
                            DBSchema.TABLE_COURSE_REMINDER_RULE,
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE
                    );
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<CourseReminderRule> getCourseReminderRules() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<CourseReminderRule> rules = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_COURSE_REMINDER_RULE,
                null
        )) {
            while (cursor.moveToNext()) {
                CourseReminderRule rule = new CourseReminderRule();
                rule.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                rule.courseId = cursor.getInt(cursor.getColumnIndexOrThrow("course_id"));
                rule.enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1;
                rule.remindDayBefore = cursor.getInt(cursor.getColumnIndexOrThrow("remind_day_before")) == 1;
                rule.remindBeforeMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("remind_before_minutes"));
                rule.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
                rules.add(rule);
            }
        }
        return rules;
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

    public List<Course> getCoursesByCourseName(String courseName) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Course> courses = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM " + DBSchema.TABLE_COURSES
                        + " WHERE course_name=?"
                        + " ORDER BY day_of_week, start_period",
                new String[]{safe(courseName)}
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

    private NotificationRule readNotificationRule(Cursor cursor) {
        NotificationRule rule = new NotificationRule();
        rule.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        rule.packageName = cursor.getString(cursor.getColumnIndexOrThrow("package_name"));
        rule.sourceName = cursor.getString(cursor.getColumnIndexOrThrow("source_name"));
        rule.groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name"));
        rule.courseId = getOptionalInt(cursor, "course_id");
        String savedCourseName = getOptionalString(cursor, "course_name");
        String joinedCourseName = getOptionalString(cursor, "bound_course_name");
        rule.courseName = isEmpty(savedCourseName) ? joinedCourseName : savedCourseName;
        rule.enabled = cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1;
        rule.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        rule.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        return rule;
    }

    private ContentValues buildNotificationRuleValues(NotificationRule rule, long now, boolean includeCreatedAt) {
        ContentValues values = new ContentValues();
        values.put("package_name", safe(rule.packageName));
        values.put("source_name", safe(rule.sourceName));
        values.put("group_name", safe(rule.groupName));
        values.put("course_id", rule.courseId);
        values.put("course_name", safe(rule.courseName));
        values.put("enabled", rule.enabled ? 1 : 0);
        if (includeCreatedAt) {
            values.put("created_at", rule.createdAt > 0 ? rule.createdAt : now);
        }
        values.put("updated_at", now);
        return values;
    }

    private static int getOptionalInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    private static String getOptionalString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getString(index) : "";
    }

    private static long getOptionalLong(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getLong(index) : 0L;
    }

    private TodoMessage readTodoMessage(Cursor cursor) {
        TodoMessage message = new TodoMessage();
        message.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        message.conversationId = cursor.getString(cursor.getColumnIndexOrThrow("conversation_id"));
        message.conversationTitle = cursor.getString(cursor.getColumnIndexOrThrow("conversation_title"));
        message.messageType = cursor.getString(cursor.getColumnIndexOrThrow("message_type"));
        message.senderType = cursor.getString(cursor.getColumnIndexOrThrow("sender_type"));
        message.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        message.content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
        message.deadline = cursor.getString(cursor.getColumnIndexOrThrow("deadline"));
        message.sourceRawId = cursor.getLong(cursor.getColumnIndexOrThrow("source_raw_id"));
        message.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        message.readAt = cursor.getLong(cursor.getColumnIndexOrThrow("read_at"));
        message.completedAt = getOptionalLong(cursor, "completed_at");
        message.reminderAt = getOptionalLong(cursor, "reminder_at");
        return message;
    }

    private RawMessage readRawMessage(Cursor cursor) {
        RawMessage msg = new RawMessage();
        msg.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        msg.sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id"));
        msg.source = parseSourceType(cursor.getString(cursor.getColumnIndexOrThrow("source_type")));
        msg.sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
        msg.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        msg.content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
        msg.rawPayload = cursor.getString(cursor.getColumnIndexOrThrow("raw_payload"));
        msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
        msg.messageHash = cursor.getString(cursor.getColumnIndexOrThrow("message_hash"));
        return msg;
    }

    private SourceType parseSourceType(String value) {
        try {
            return SourceType.valueOf(value);
        } catch (Exception ignored) {
            return SourceType.ANDROID;
        }
    }

    private long getConversationPinnedAt(SQLiteDatabase db, String conversationId) {
        try (Cursor cursor = db.rawQuery(
                "SELECT MAX(pinned_at) FROM " + DBSchema.TABLE_TODO_MESSAGE
                        + " WHERE conversation_id=?",
                new String[]{safe(conversationId)}
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception ignored) {
        }
        return 0;
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

    public void close() {
        helper.close();
    }
}
