package db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

import model.RawMessage;

public class DBManager {

    private DBHelper helper;

    public DBManager(Context context) {
        helper = new DBHelper(context);
    }

    // =========================
    // 1. insert raw message
    // =========================
    public void insertRawMessage(RawMessage msg) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("session_id", msg.sessionId);
        values.put("source_type", msg.source.name());
        values.put("sender", msg.sender);
        values.put("title", msg.title);
        values.put("content", msg.content);
        values.put("raw_payload", msg.rawPayload);
        values.put("timestamp", msg.timestamp);
        values.put("message_hash", msg.messageHash);

        // 去重插入（冲突则忽略）
        db.insertWithOnConflict(
                "raw_message",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    // =========================
    // 2. get recent messages
    // =========================
    public List<RawMessage> getRecentMessages(
            String sessionId,
            int limit
    ) {

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM raw_message "
                + "WHERE session_id=? "
                + "ORDER BY timestamp DESC "
                + "LIMIT ?",
                new String[]{
                    sessionId,
                    String.valueOf(limit)
                }
        );

        List<RawMessage> list = new ArrayList<>();

        while (cursor.moveToNext()) {

            RawMessage msg = new RawMessage();

            msg.sessionId = cursor.getString(cursor.getColumnIndex("session_id"));
            msg.sender = cursor.getString(cursor.getColumnIndex("sender"));
            msg.title = cursor.getString(cursor.getColumnIndex("title"));
            msg.content = cursor.getString(cursor.getColumnIndex("content"));
            msg.rawPayload = cursor.getString(cursor.getColumnIndex("raw_payload"));
            msg.timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));

            list.add(msg);
        }

        cursor.close();

        return list;
    }

    // =========================
    // 4. insert normalized result
    // =========================
    public long insertNormalizedRecord(
            String courseName,
            String taskTitle,
            String description,
            String deadline,
            double confidence
    ) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("course_name", courseName);
        values.put("task_title", taskTitle);
        values.put("description", description);
        values.put("deadline", deadline);
        values.put("confidence", confidence);
        values.put("created_at", System.currentTimeMillis());
        values.put("updated_at", System.currentTimeMillis());

        return db.insert(
                "normalized_record",
                null,
                values
        );
    }

    // =========================
    // 5. link raw <-> normalized
    // =========================
    public void linkRawToNormalized(long rawId, long normalizedId) {

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("raw_id", rawId);
        values.put("normalized_id", normalizedId);

        db.insert(
                "raw_normalized_map",
                null,
                values
        );
    }
}
