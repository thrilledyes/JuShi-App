package com.jushi.demo.main.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.jushi.demo.main.data.entity.Course;

@Database(entities = {Course.class}, version = 1, exportSchema = false)
public abstract class CourseDatabase extends RoomDatabase {

    private static volatile CourseDatabase instance;

    public abstract CourseDao courseDao();

    public static CourseDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CourseDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CourseDatabase.class,
                            "jushi_courses.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
