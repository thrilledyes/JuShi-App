package com.jushi.demo.main.ui.timetable;

import android.content.Context;
import android.content.SharedPreferences;

import com.jushi.demo.main.data.entity.Course;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeekUtils {

    private static final String PREFS_NAME = "jushi_timetable_prefs";
    private static final String KEY_SEMESTER_START = "semester_start_date";
    private static final String KEY_CURRENT_WEEK = "current_week";

    // Default: Feb 17, 2026 (approximate spring semester start, a Monday)
    private static final long DEFAULT_SEMESTER_START = getDefaultSemesterStart();

    private static long getDefaultSemesterStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 17, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getSemesterStart(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_SEMESTER_START, DEFAULT_SEMESTER_START);
    }

    public static void setSemesterStart(Context context, long timestamp) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_SEMESTER_START, timestamp).apply();
    }

    public static int getCurrentWeek(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_CURRENT_WEEK, 1);
    }

    public static void setCurrentWeek(Context context, int week) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_CURRENT_WEEK, week).apply();
    }

    public static boolean isActiveInWeek(Course course, int week) {
        String range = course.getWeekRange();
        if (range == null || range.isEmpty()) return true;

        try {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                return week >= start && week <= end;
            }
            for (String s : range.split(",")) {
                if (Integer.parseInt(s.trim()) == week) return true;
            }
        } catch (NumberFormatException e) {
            return true;
        }
        return false;
    }

    public static String[] getDateLabels(int weekNum, long semesterStart) {
        String[] labels = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("M/d", Locale.getDefault());
        long msPerDay = 86400000L;
        long base = semesterStart + (weekNum - 1) * 7L * msPerDay;

        for (int i = 0; i < 7; i++) {
            labels[i] = sdf.format(new Date(base + i * msPerDay));
        }
        return labels;
    }

    public static String getWeekDateRange(int weekNum, long semesterStart) {
        SimpleDateFormat sdf = new SimpleDateFormat("M/d", Locale.getDefault());
        long msPerDay = 86400000L;
        long base = semesterStart + (weekNum - 1) * 7L * msPerDay;
        String start = sdf.format(new Date(base));
        String end = sdf.format(new Date(base + 6 * msPerDay));
        return start + " - " + end;
    }

    public static final String[] DAY_NAMES = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    public static final String[] DAY_FULL_NAMES = {"", "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    public static final int[][] PERIOD_TIMES = {
            {0, 0},           // placeholder
            {8*60, 8*60+45},     // 1: 08:00~08:45
            {8*60+55, 9*60+40},  // 2: 08:55~09:40
            {10*60+10, 10*60+55},// 3: 10:10~10:55
            {11*60+5, 11*60+50}, // 4: 11:05~11:50
            {14*60+20, 15*60+5}, // 5: 14:20~15:05
            {15*60+15, 16*60},   // 6: 15:15~16:00
            {16*60+30, 17*60+15},// 7: 16:30~17:15
            {17*60+25, 18*60+10},// 8: 17:25~18:10
            {19*60, 19*60+45},   // 9: 19:00~19:45
            {19*60+55, 20*60+40},// 10: 19:55~20:40
            {20*60+50, 21*60+35},// 11: 20:50~21:35
    };

    public static final int MAX_PERIODS = 11;

    public static String getPeriodTimeText(int period) {
        if (period < 1 || period > MAX_PERIODS) return "";
        int start = PERIOD_TIMES[period][0];
        int end = PERIOD_TIMES[period][1];
        return String.format(Locale.getDefault(), "%d:%02d~%d:%02d",
                start / 60, start % 60, end / 60, end % 60);
    }

    public static String getPeriodStartTimeText(int period) {
        if (period < 1 || period > MAX_PERIODS) return "";
        int minutes = PERIOD_TIMES[period][0];
        return String.format(Locale.getDefault(), "%02d:%02d",
                minutes / 60, minutes % 60);
    }
}
