package com.jushi.demo.main.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jushi.demo.main.data.entity.Course;

import java.util.List;

@Dao
public interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY day_of_week, start_period")
    List<Course> getAllCourses();

    @Query("SELECT * FROM courses WHERE day_of_week = :dayOfWeek ORDER BY start_period")
    List<Course> getByDayOfWeek(int dayOfWeek);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Course> courses);

    @Query("DELETE FROM courses")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM courses")
    int getCount();
}
