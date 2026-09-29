package com.jushi.demo.main.database.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "catcher.db";
    private static final int DB_VERSION = 7;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureLatestSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(DBSchema.CREATE_COURSES);
            db.execSQL(DBSchema.CREATE_NOTIFICATION_RULE);
        }
        if (oldVersion < 3) {
            db.execSQL(DBSchema.CREATE_TODO_MESSAGE);
            db.execSQL(DBSchema.CREATE_COURSE_REMINDER_RULE);
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + DBSchema.TABLE_TODO_MESSAGE
                        + " ADD COLUMN pinned_at INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + DBSchema.TABLE_NOTIFICATION_RULE
                        + " ADD COLUMN course_id INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + DBSchema.TABLE_NOTIFICATION_RULE
                        + " ADD COLUMN course_name TEXT");
            } catch (Exception ignored) {
            }
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE " + DBSchema.TABLE_TODO_MESSAGE
                        + " ADD COLUMN completed_at INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + DBSchema.TABLE_TODO_MESSAGE
                        + " ADD COLUMN reminder_at INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON;");
        ensureLatestSchema(db);
    }

    private void ensureLatestSchema(SQLiteDatabase db) {
        db.execSQL(DBSchema.CREATE_CONVERSATION);
        db.execSQL(DBSchema.CREATE_RAW_MESSAGE);
        db.execSQL(DBSchema.CREATE_NORMALIZED_RECORD);
        db.execSQL(DBSchema.CREATE_RAW_NORMALIZED_MAP);
        db.execSQL(DBSchema.CREATE_AUTH_SESSION);
        db.execSQL(DBSchema.CREATE_COURSES);
        db.execSQL(DBSchema.CREATE_NOTIFICATION_RULE);
        db.execSQL(DBSchema.CREATE_TODO_MESSAGE);
        db.execSQL(DBSchema.CREATE_COURSE_REMINDER_RULE);

        addColumnIfMissing(db, DBSchema.TABLE_CONVERSATION, "source_type TEXT");
        addColumnIfMissing(db, DBSchema.TABLE_CONVERSATION, "name TEXT");
        addColumnIfMissing(db, DBSchema.TABLE_CONVERSATION, "last_active INTEGER");

        addColumnIfMissing(db, DBSchema.TABLE_RAW_MESSAGE, "title TEXT");
        addColumnIfMissing(db, DBSchema.TABLE_RAW_MESSAGE, "raw_payload TEXT");
        addColumnIfMissing(db, DBSchema.TABLE_RAW_MESSAGE, "message_hash TEXT");
        execIgnore(db, "CREATE UNIQUE INDEX IF NOT EXISTS idx_raw_message_hash"
                + " ON " + DBSchema.TABLE_RAW_MESSAGE + "(message_hash)");

        addColumnIfMissing(db, DBSchema.TABLE_NOTIFICATION_RULE, "course_id INTEGER DEFAULT 0");
        addColumnIfMissing(db, DBSchema.TABLE_NOTIFICATION_RULE, "course_name TEXT");

        addColumnIfMissing(db, DBSchema.TABLE_TODO_MESSAGE, "read_at INTEGER DEFAULT 0");
        addColumnIfMissing(db, DBSchema.TABLE_TODO_MESSAGE, "pinned_at INTEGER DEFAULT 0");
        addColumnIfMissing(db, DBSchema.TABLE_TODO_MESSAGE, "completed_at INTEGER DEFAULT 0");
        addColumnIfMissing(db, DBSchema.TABLE_TODO_MESSAGE, "reminder_at INTEGER DEFAULT 0");
    }

    private static void addColumnIfMissing(SQLiteDatabase db, String tableName, String columnDefinition) {
        execIgnore(db, "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
    }

    private static void execIgnore(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception ignored) {
        }
    }
}
