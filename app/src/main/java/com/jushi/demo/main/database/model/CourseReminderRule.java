package com.jushi.demo.main.database.model;

public class CourseReminderRule {
    public long id;
    public int courseId;
    public boolean enabled;
    public boolean remindDayBefore;
    public int remindBeforeMinutes;
    public long updatedAt;
}
