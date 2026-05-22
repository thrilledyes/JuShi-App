package com.jushi.demo.main.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "course_name")
    private String courseName;

    @ColumnInfo(name = "teacher")
    private String teacher;

    @ColumnInfo(name = "location")
    private String location;

    @ColumnInfo(name = "day_of_week")
    private int dayOfWeek; // 1=Sunday ... 7=Saturday

    @ColumnInfo(name = "start_period")
    private int startPeriod;

    @ColumnInfo(name = "end_period")
    private int endPeriod;

    @ColumnInfo(name = "week_range")
    private String weekRange; // e.g., "1-17" or "4-4"

    private int color;

    public Course() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getStartPeriod() { return startPeriod; }
    public void setStartPeriod(int startPeriod) { this.startPeriod = startPeriod; }

    public int getEndPeriod() { return endPeriod; }
    public void setEndPeriod(int endPeriod) { this.endPeriod = endPeriod; }

    public String getWeekRange() { return weekRange; }
    public void setWeekRange(String weekRange) { this.weekRange = weekRange; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
