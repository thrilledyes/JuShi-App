package db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    /*
     * =========================
     * Database Config
     * =========================
     */
    private static final String DB_NAME = "catcher.db";

    private static final int DB_VERSION = 1;


    /*
     * Constructor
     */
    public DBHelper(Context context) {

        super(
                context,
                DB_NAME,
                null,
                DB_VERSION
        );

    }


    /*
     * =========================
     * Create Tables
     * =========================
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // conversation
        db.execSQL(DBSchema.CREATE_CONVERSATION);

        // raw messages (core data)
        db.execSQL(DBSchema.CREATE_RAW_MESSAGE);

        // AI structured result
        db.execSQL(DBSchema.CREATE_NORMALIZED_RECORD);

        // mapping raw <-> normalized (many-to-many)
        db.execSQL(DBSchema.CREATE_RAW_NORMALIZED_MAP);

        // login/session storage
        db.execSQL(DBSchema.CREATE_AUTH_SESSION);

    }


    /*
     * =========================
     * Upgrade Strategy
     * =========================
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

        /*
         * Future migration strategy:
         *
         * NEVER drop tables blindly in production.
         * Instead:
         * 1. add new columns
         * 2. migrate data
         * 3. keep backward compatibility
         */
        if (oldVersion < 2) {

            // example future update:
            // db.execSQL("ALTER TABLE raw_message ADD COLUMN message_hash TEXT");
        }

        if (oldVersion < 3) {

            // future normalized schema update
        }

    }


    /*
     * =========================
     * Optional Safety Hook
     * =========================
     */
    @Override
    public void onOpen(SQLiteDatabase db) {

        super.onOpen(db);

        /*
         * Good practice:
         * enable foreign key constraints (if later used)
         */
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

}
