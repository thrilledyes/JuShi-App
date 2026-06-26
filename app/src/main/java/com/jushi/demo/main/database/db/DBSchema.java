package com.jushi.demo.main.database.db;

public final class DBSchema {
    private DBSchema() {
    }

    public static final String TABLE_CONVERSATION = "conversation";
    public static final String TABLE_RAW_MESSAGE = "raw_message";
    public static final String TABLE_NORMALIZED_RECORD = "normalized_record";
    public static final String TABLE_RAW_NORMALIZED_MAP = "raw_normalized_map";
    public static final String TABLE_AUTH_SESSION = "auth_session";
    public static final String TABLE_COURSES = "courses";
    public static final String TABLE_NOTIFICATION_RULE = "notification_rule";
    public static final String TABLE_TODO_MESSAGE = "todo_message";
    public static final String TABLE_COURSE_REMINDER_RULE = "course_reminder_rule";

    public static final String CREATE_CONVERSATION =
            "CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATION + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "session_id TEXT UNIQUE,"
                    + "source_type TEXT,"
                    + "name TEXT,"
                    + "last_active INTEGER"
                    + ")";

    public static final String CREATE_RAW_MESSAGE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RAW_MESSAGE + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "session_id TEXT,"
                    + "source_type TEXT,"
                    + "sender TEXT,"
                    + "title TEXT,"
                    + "content TEXT,"
                    + "raw_payload TEXT,"
                    + "timestamp INTEGER,"
                    + "message_hash TEXT UNIQUE"
                    + ")";

    public static final String CREATE_NORMALIZED_RECORD =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NORMALIZED_RECORD + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "course_name TEXT,"
                    + "task_title TEXT,"
                    + "description TEXT,"
                    + "deadline TEXT,"
                    + "confidence REAL,"
                    + "created_at INTEGER,"
                    + "updated_at INTEGER"
                    + ")";

    public static final String CREATE_RAW_NORMALIZED_MAP =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RAW_NORMALIZED_MAP + "("
                    + "raw_id INTEGER,"
                    + "normalized_id INTEGER,"
                    + "PRIMARY KEY(raw_id, normalized_id)"
                    + ")";

    public static final String CREATE_AUTH_SESSION =
            "CREATE TABLE IF NOT EXISTS " + TABLE_AUTH_SESSION + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "platform TEXT,"
                    + "account TEXT,"
                    + "cookie TEXT,"
                    + "token TEXT,"
                    + "login_payload TEXT,"
                    + "expire_time INTEGER,"
                    + "updated_at INTEGER"
                    + ")";

    public static final String CREATE_COURSES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_COURSES + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "course_name TEXT,"
                    + "teacher TEXT,"
                    + "location TEXT,"
                    + "day_of_week INTEGER,"
                    + "start_period INTEGER,"
                    + "end_period INTEGER,"
                    + "week_range TEXT,"
                    + "color INTEGER"
                    + ")";

    public static final String CREATE_NOTIFICATION_RULE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATION_RULE + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "package_name TEXT,"
                    + "source_name TEXT,"
                    + "group_name TEXT,"
                    + "course_id INTEGER DEFAULT 0,"
                    + "course_name TEXT,"
                    + "enabled INTEGER DEFAULT 1,"
                    + "created_at INTEGER,"
                    + "updated_at INTEGER"
                    + ")";

    public static final String CREATE_TODO_MESSAGE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_TODO_MESSAGE + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "conversation_id TEXT,"
                    + "conversation_title TEXT,"
                    + "message_type TEXT,"
                    + "sender_type TEXT,"
                    + "title TEXT,"
                    + "content TEXT,"
                    + "deadline TEXT,"
                    + "source_raw_id INTEGER,"
                    + "created_at INTEGER,"
                    + "read_at INTEGER DEFAULT 0,"
                    + "pinned_at INTEGER DEFAULT 0,"
                    + "completed_at INTEGER DEFAULT 0,"
                    + "reminder_at INTEGER DEFAULT 0"
                    + ")";

    public static final String CREATE_COURSE_REMINDER_RULE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_COURSE_REMINDER_RULE + "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "course_id INTEGER UNIQUE,"
                    + "enabled INTEGER DEFAULT 1,"
                    + "remind_day_before INTEGER DEFAULT 1,"
                    + "remind_before_minutes INTEGER DEFAULT 20,"
                    + "updated_at INTEGER"
                    + ")";
}
