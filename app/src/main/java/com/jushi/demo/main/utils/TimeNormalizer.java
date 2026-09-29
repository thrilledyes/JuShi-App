package com.jushi.demo.main.utils;

import android.content.Context;
import android.util.Log;

import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.ui.timetable.WeekUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeNormalizer {
    private static final String TAG = "TimeNormalizer";

    public static String normalize(Context context, String rawTime, String groupName) {
        return normalize(context, rawTime, groupName, "");
    }

    public static String normalize(Context context, String rawTime, String groupName, String boundCourseName) {
        if (rawTime == null) return null;

        String timeStr = rawTime.trim()
                .replace("\u2005", "")
                .replace(" ", "")
                .replace("：", ":");
        if (timeStr.isEmpty() || timeStr.contains("未检测") || timeStr.contains("越界")) {
            return null;
        }

        if (containsNextClassExpression(timeStr)) {
            String nextClassTime = getNextClassTime(context, groupName, boundCourseName);
            if (nextClassTime != null) {
                return nextClassTime;
            }
        }

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        if (timeStr.contains("今天") || timeStr.contains("今晚") || timeStr.contains("今日")) {
            return formatDate(cal) + extractTimeSuffix(timeStr);
        }
        if (timeStr.contains("明天") || timeStr.contains("明晚") || timeStr.contains("明日")) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return formatDate(cal) + extractTimeSuffix(timeStr);
        }
        if (timeStr.contains("后天")) {
            cal.add(Calendar.DAY_OF_YEAR, 2);
            return formatDate(cal) + extractTimeSuffix(timeStr);
        }

        timeStr = timeStr.replace("24点", "23:59")
                .replace("24:00", "23:59")
                .replace("晚上", "")
                .replace("晚", "");

        Matcher fullDate = Pattern.compile("(\\d{4})[\\.-](\\d{1,2})[\\.-](\\d{1,2})(?:.*?(\\d{1,2}:\\d{1,2}(?::\\d{1,2})?))?").matcher(timeStr);
        if (fullDate.find()) {
            return formatStandard(fullDate.group(1), fullDate.group(2), fullDate.group(3), extractExplicitTime(rawTime));
        }

        Matcher monthDay = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]?(?:.*?(\\d{1,2}:\\d{1,2}(?::\\d{1,2})?))?").matcher(timeStr);
        if (monthDay.find()) {
            return formatStandard(String.valueOf(currentYear), monthDay.group(1), monthDay.group(2), extractExplicitTime(rawTime));
        }

        Matcher weekDay = Pattern.compile("(本周|下周)([一二三四五六日天])(?:.*?(\\d{1,2}:\\d{1,2}))?").matcher(timeStr);
        if (weekDay.find()) {
            boolean isNextWeek = "下周".equals(weekDay.group(1));
            int dayOfWeek = parseDayOfWeek(weekDay.group(2));
            cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            if (isNextWeek || cal.getTimeInMillis() < System.currentTimeMillis() - 86400000L) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            }
            return formatDate(cal) + extractTimeSuffix(timeStr);
        }

        if (timeStr.matches(".*\\d+.*")) {
            return rawTime.trim();
        }
        return null;
    }

    private static boolean containsNextClassExpression(String timeStr) {
        return timeStr.contains("下节")
                || timeStr.contains("下次")
                || timeStr.contains("下堂")
                || timeStr.contains("下节课")
                || timeStr.contains("下次课")
                || timeStr.contains("下堂课");
    }

    private static String getNextClassTime(Context context, String groupName, String boundCourseName) {
        DBManager dbManager = null;
        try {
            dbManager = new DBManager(context);
            List<Course> allCourses = dbManager.getAllCourses();
            long now = System.currentTimeMillis();
            long semesterStart = WeekUtils.getSemesterStart(context);
            boolean hasBoundCourse = !isEmpty(boundCourseName);
            long bestTime = hasBoundCourse
                    ? findNextCourseTime(allCourses, now, semesterStart, groupName, boundCourseName, true)
                    : Long.MAX_VALUE;

            if (bestTime == Long.MAX_VALUE) {
                bestTime = findNextCourseTime(allCourses, now, semesterStart, groupName, boundCourseName, false);
            }

            if (bestTime != Long.MAX_VALUE) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new Date(bestTime));
            }
        } catch (Exception e) {
            Log.e(TAG, "查询课表数据库失败", e);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
        return null;
    }

    private static long findNextCourseTime(
            List<Course> allCourses,
            long now,
            long semesterStart,
            String groupName,
            String boundCourseName,
            boolean useBoundCourse
    ) {
        if (allCourses == null || allCourses.isEmpty()) {
            return Long.MAX_VALUE;
        }
        if (!useBoundCourse && isEmpty(groupName)) {
            return Long.MAX_VALUE;
        }

        long bestTime = Long.MAX_VALUE;
        int currentWeek = WeekUtils.calculateWeekForDate(now, semesterStart);
        for (Course course : allCourses) {
            boolean matched = useBoundCourse
                    ? matchesBoundCourse(boundCourseName, course)
                    : matchesCourse(groupName, course);
            if (!matched) {
                continue;
            }

            for (int week = currentWeek; week <= 30; week++) {
                if (!WeekUtils.isActiveInWeek(course, week)) {
                    continue;
                }
                long classTime = buildClassStartMillis(course, week, semesterStart);
                if (classTime > now && classTime < bestTime) {
                    bestTime = classTime;
                }
            }
        }
        return bestTime;
    }

    private static boolean matchesBoundCourse(String boundCourseName, Course course) {
        if (isEmpty(boundCourseName) || course == null || isEmpty(course.getCourseName())) {
            return false;
        }
        return normalizeText(course.getCourseName()).equals(normalizeText(boundCourseName));
    }

    private static boolean matchesCourse(String groupName, Course course) {
        if (isEmpty(groupName) || course == null) {
            return false;
        }
        String courseName = course.getCourseName();
        if (courseName == null || courseName.trim().isEmpty()) {
            return false;
        }
        String normalizedGroup = normalizeText(groupName);
        String normalizedCourse = normalizeText(courseName);
        return normalizedGroup.contains(normalizedCourse)
                || (normalizedCourse.length() >= 4 && normalizedCourse.contains(normalizedGroup));
    }

    private static long buildClassStartMillis(Course course, int week, long semesterStart) {
        int dayIdx = course.getDayOfWeek() == 1 ? 6 : course.getDayOfWeek() - 2;
        if (dayIdx < 0 || dayIdx > 6) {
            return Long.MAX_VALUE;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(semesterStart + (week - 1L) * 7L * 86400000L + dayIdx * 86400000L);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int period = course.getStartPeriod();
        if (period < 1 || period >= WeekUtils.PERIOD_TIMES.length) {
            return Long.MAX_VALUE;
        }
        int startMinutes = WeekUtils.PERIOD_TIMES[period][0];
        cal.add(Calendar.MINUTE, startMinutes);
        return cal.getTimeInMillis();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s_\\-—:：,，.。()（）\\[\\]【】]", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String formatStandard(String y, String m, String d, String time) {
        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
        return date + " " + (time == null || time.isEmpty() ? "23:59" : time);
    }

    private static String formatDate(Calendar cal) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private static int parseDayOfWeek(String dayStr) {
        switch (dayStr) {
            case "一": return Calendar.MONDAY;
            case "二": return Calendar.TUESDAY;
            case "三": return Calendar.WEDNESDAY;
            case "四": return Calendar.THURSDAY;
            case "五": return Calendar.FRIDAY;
            case "六": return Calendar.SATURDAY;
            case "日":
            case "天": return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }

    private static String extractTimeSuffix(String timeStr) {
        String explicit = extractExplicitTime(timeStr);
        return explicit == null ? " 23:59" : " " + explicit;
    }

    private static String extractExplicitTime(String timeStr) {
        String value = timeStr == null ? "" : timeStr.replace("：", ":");
        boolean evening = value.contains("晚")
                || value.contains("下午")
                || value.contains("傍晚")
                || value.contains("今晚")
                || value.contains("明晚");

        Matcher colonTime = Pattern.compile("(\\d{1,2}):(\\d{1,2})(?::\\d{1,2})?").matcher(value);
        if (colonTime.find()) {
            int hour = Integer.parseInt(colonTime.group(1));
            int minute = Integer.parseInt(colonTime.group(2));
            if (hour >= 24) {
                return "23:59";
            }
            if (evening && hour > 0 && hour < 12) {
                hour += 12;
            }
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        }

        Matcher hour = Pattern.compile("(\\d{1,2})点(?:(\\d{1,2})分?)?").matcher(value);
        if (hour.find()) {
            int hourValue = Integer.parseInt(hour.group(1));
            int minuteValue = hour.group(2) == null ? 0 : Integer.parseInt(hour.group(2));
            if (hourValue >= 24) {
                return "23:59";
            }
            if (evening && hourValue > 0 && hourValue < 12) {
                hourValue += 12;
            }
            return String.format(Locale.getDefault(), "%02d:%02d", hourValue, minuteValue);
        }
        return null;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
