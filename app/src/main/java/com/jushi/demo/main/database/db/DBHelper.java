package com.jushi.demo.main.database.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "catcher.db";
    private static final int DB_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBSchema.CREATE_CONVERSATION);
        db.execSQL(DBSchema.CREATE_RAW_MESSAGE);
        db.execSQL(DBSchema.CREATE_NORMALIZED_RECORD);
        db.execSQL(DBSchema.CREATE_RAW_NORMALIZED_MAP);
        db.execSQL(DBSchema.CREATE_AUTH_SESSION);
        db.execSQL(DBSchema.CREATE_COURSES);
        db.execSQL(DBSchema.CREATE_NOTIFICATION_RULE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(DBSchema.CREATE_COURSES);
            db.execSQL(DBSchema.CREATE_NOTIFICATION_RULE);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON;");
    }
}
