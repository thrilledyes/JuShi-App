package com.jushi.demo.main.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.ui.timetable.WeekUtils;

import java.util.List;

public class CourseReminderReceiver extends BroadcastReceiver {
    private static final String COURSE_ASSISTANT_ID = "course_assistant";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            CourseReminderScheduler.scheduleAll(context);
            return;
        }

        int courseId = intent.getIntExtra(CourseReminderScheduler.EXTRA_COURSE_ID, -1);
        String kind = intent.getStringExtra(CourseReminderScheduler.EXTRA_REMINDER_KIND);
        if (courseId <= 0) {
            return;
        }

        DBManager db = new DBManager(context.getApplicationContext());
        Course course = null;
        try {
            List<Course> courses = db.getAllCourses();
            for (Course item : courses) {
                if (item.getId() == courseId) {
                    course = item;
                    break;
                }
            }
            if (course == null) {
                return;
            }

            String title = CourseReminderScheduler.KIND_DAY_BEFORE.equals(kind)
                    ? "明天有课提醒"
                    : "上课前提醒";
            db.insertTodoMessage(
                    COURSE_ASSISTANT_ID,
                    "课程表助手",
                    "course_reminder",
                    "bot",
                    title,
                    buildMessage(course, kind),
                    "",
                    -1
            );
        } finally {
            db.close();
        }

        CourseReminderScheduler.scheduleNextForCourse(context, courseId);
    }

    private String buildMessage(Course course, String kind) {
        String prefix = CourseReminderScheduler.KIND_DAY_BEFORE.equals(kind)
                ? "明天这节课需要留意："
                : "20 分钟后准备上课：";
        StringBuilder sb = new StringBuilder(prefix);
        sb.append("\n").append(course.getCourseName());
        sb.append("\n").append(dayName(course.getDayOfWeek()));
        sb.append(" 第").append(course.getStartPeriod()).append("-").append(course.getEndPeriod()).append("节");
        String time = WeekUtils.getPeriodTimeText(course.getStartPeriod());
        if (time != null && !time.isEmpty()) {
            sb.append(" ").append(time);
        }
        if (course.getLocation() != null && !course.getLocation().isEmpty()) {
            sb.append("\n地点：").append(course.getLocation());
        }
        if (course.getTeacher() != null && !course.getTeacher().isEmpty()) {
            sb.append("\n教师：").append(course.getTeacher());
        }
        return sb.toString();
    }

    private String dayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1:
                return "周日";
            case 2:
                return "周一";
            case 3:
                return "周二";
            case 4:
                return "周三";
            case 5:
                return "周四";
            case 6:
                return "周五";
            case 7:
                return "周六";
            default:
                return "";
        }
    }
}
