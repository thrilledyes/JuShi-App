package com.jushi.demo.main.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.CourseReminderRule;
import com.jushi.demo.main.ui.timetable.WeekUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CourseReminderScheduler {
    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_REMINDER_KIND = "reminder_kind";
    public static final String KIND_DAY_BEFORE = "day_before";
    public static final String KIND_BEFORE_CLASS = "before_class";

    private CourseReminderScheduler() {
    }

    public static void scheduleAll(Context context) {
        Context appContext = context.getApplicationContext();
        DBManager db = new DBManager(appContext);
        List<Course> courses;
        List<CourseReminderRule> rules;
        try {
            courses = db.getAllCourses();
            rules = db.getCourseReminderRules();
        } finally {
            db.close();
        }

        for (Course course : courses) {
            cancel(appContext, course.getId(), KIND_DAY_BEFORE);
            cancel(appContext, course.getId(), KIND_BEFORE_CLASS);
        }

        Map<Integer, Course> courseMap = new HashMap<>();
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }

        for (CourseReminderRule rule : rules) {
            if (!rule.enabled) {
                continue;
            }
            Course course = courseMap.get(rule.courseId);
            if (course == null) {
                continue;
            }
            if (rule.remindDayBefore) {
                scheduleOne(appContext, course, KIND_DAY_BEFORE, 0);
            }
            if (rule.remindBeforeMinutes > 0) {
                scheduleOne(appContext, course, KIND_BEFORE_CLASS, rule.remindBeforeMinutes);
            }
        }
    }

    static void scheduleNextForCourse(Context context, int courseId) {
        DBManager db = new DBManager(context.getApplicationContext());
        Course target = null;
        List<CourseReminderRule> rules;
        try {
            for (Course course : db.getAllCourses()) {
                if (course.getId() == courseId) {
                    target = course;
                    break;
                }
            }
            rules = db.getCourseReminderRules();
        } finally {
            db.close();
        }
        if (target == null) {
            return;
        }
        for (CourseReminderRule rule : rules) {
            if (rule.courseId != courseId || !rule.enabled) {
                continue;
            }
            if (rule.remindDayBefore) {
                scheduleOne(context, target, KIND_DAY_BEFORE, 0);
            }
            if (rule.remindBeforeMinutes > 0) {
                scheduleOne(context, target, KIND_BEFORE_CLASS, rule.remindBeforeMinutes);
            }
        }
    }

    public static boolean hasFutureCourseOccurrence(Context context, Course course) {
        return findNextClassStartTime(context, course) > 0;
    }

    private static void scheduleOne(Context context, Course course, String kind, int beforeMinutes) {
        long triggerAt = findNextTriggerTime(context, course, kind, beforeMinutes);
        if (triggerAt <= 0) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildPendingIntent(context, course.getId(), kind);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static void cancel(Context context, int courseId, String kind) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(buildPendingIntent(context, courseId, kind));
        }
    }

    private static PendingIntent buildPendingIntent(Context context, int courseId, String kind) {
        Intent intent = new Intent(context, CourseReminderReceiver.class);
        intent.putExtra(EXTRA_COURSE_ID, courseId);
        intent.putExtra(EXTRA_REMINDER_KIND, kind);
        int requestCode = 31 * courseId + kind.hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private static long findNextTriggerTime(Context context, Course course, String kind, int beforeMinutes) {
        long now = System.currentTimeMillis();
        long semesterStart = WeekUtils.getSemesterStart(context);
        Calendar calendar = Calendar.getInstance();

        for (int week = 1; week <= 30; week++) {
            if (!WeekUtils.isActiveInWeek(course, week)) {
                continue;
            }

            long classDay = semesterStart
                    + (week - 1L) * 7L * 24L * 60L * 60L * 1000L
                    + dayOffset(course.getDayOfWeek()) * 24L * 60L * 60L * 1000L;
            calendar.setTimeInMillis(classDay);
            int startMinutes = periodStartMinutes(course.getStartPeriod());
            calendar.set(Calendar.HOUR_OF_DAY, startMinutes / 60);
            calendar.set(Calendar.MINUTE, startMinutes % 60);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (KIND_DAY_BEFORE.equals(kind)) {
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                calendar.set(Calendar.HOUR_OF_DAY, 22);
                calendar.set(Calendar.MINUTE, 0);
            } else {
                calendar.add(Calendar.MINUTE, -beforeMinutes);
            }

            long trigger = calendar.getTimeInMillis();
            if (trigger > now + 30_000L) {
                return trigger;
            }
        }
        return -1L;
    }

    private static long findNextClassStartTime(Context context, Course course) {
        long now = System.currentTimeMillis();
        long semesterStart = WeekUtils.getSemesterStart(context);
        Calendar calendar = Calendar.getInstance();

        for (int week = 1; week <= 30; week++) {
            if (!WeekUtils.isActiveInWeek(course, week)) {
                continue;
            }

            long classDay = semesterStart
                    + (week - 1L) * 7L * 24L * 60L * 60L * 1000L
                    + dayOffset(course.getDayOfWeek()) * 24L * 60L * 60L * 1000L;
            calendar.setTimeInMillis(classDay);
            int startMinutes = periodStartMinutes(course.getStartPeriod());
            calendar.set(Calendar.HOUR_OF_DAY, startMinutes / 60);
            calendar.set(Calendar.MINUTE, startMinutes % 60);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long classStart = calendar.getTimeInMillis();
            if (classStart > now) {
                return classStart;
            }
        }
        return -1L;
    }

    private static int dayOffset(int dayOfWeek) {
        return (dayOfWeek + 5) % 7;
    }

    private static int periodStartMinutes(int period) {
        if (period < 1 || period >= WeekUtils.PERIOD_TIMES.length) {
            return 8 * 60;
        }
        return WeekUtils.PERIOD_TIMES[period][0];
    }
}
