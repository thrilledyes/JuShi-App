package com.jushi.demo.main.chaosuan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class EasyHpcHomeworkNormalizer {
    private EasyHpcHomeworkNormalizer() {
    }

    public static List<EasyHpcHomework> normalize(
            String courseId,
            String courseName,
            List<JSONObject> tasks
    ) {
        List<EasyHpcHomework> records = new ArrayList<>();
        if (tasks == null) {
            return records;
        }

        for (JSONObject task : tasks) {
            if (task == null) {
                continue;
            }

            EasyHpcHomework record = new EasyHpcHomework();
            record.courseId = courseId;
            record.courseName = cleanText(courseName);
            record.homeworkId = stringValue(firstExisting(task, "ID", "Id", "id", "HomeworkID", "homeworkId"));
            record.taskTitle = cleanText(stringValue(firstExisting(
                    task,
                    "title",
                    "Title",
                    "taskTitle",
                    "TaskTitle",
                    "Name",
                    "name"
            )));
            record.description = cleanText(stringValue(firstExisting(
                    task,
                    "description",
                    "Description",
                    "content",
                    "Content",
                    "detail",
                    "Detail"
            )));
            Object deadlineValue = firstExisting(
                    task,
                    "ddl",
                    "DDL",
                    "due_time",
                    "dueTime",
                    "DueTime",
                    "due_at",
                    "DueAt",
                    "deadline",
                    "Deadline",
                    "finish_time",
                    "FinishTime",
                    "end_time",
                    "EndTime"
            );
            record.deadline = normalizeDeadline(deadlineValue);
            Object createdValue = firstExisting(
                    task,
                    "created_at",
                    "createdAt",
                    "CreatedAt",
                    "create_time",
                    "createTime",
                    "CreateTime",
                    "publish_time",
                    "publishTime",
                    "PublishTime",
                    "published_at",
                    "publishedAt",
                    "release_time",
                    "releaseTime",
                    "ReleaseTime",
                    "ctime",
                    "CTime",
                    "add_time",
                    "addTime",
                    "AddTime"
            );
            record.createdAt = normalizeTimestamp(createdValue);
            record.confidence = isEmpty(record.deadline) ? 0.3 : 0.8;
            record.rawPayload = task.toString();

            if (!isEmpty(record.taskTitle) || !isEmpty(record.description) || !isEmpty(record.deadline)) {
                records.add(record);
            }
        }

        return records;
    }

    public static JSONArray extractArray(Object response) {
        if (response instanceof JSONArray) {
            return (JSONArray) response;
        }
        if (!(response instanceof JSONObject)) {
            return new JSONArray();
        }

        JSONObject object = (JSONObject) response;
        Object value = firstExisting(object, "data", "Data", "list", "List", "items", "Items");
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof JSONObject) {
            JSONArray nested = extractArray(value);
            if (nested.length() > 0) {
                return nested;
            }
        }

        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            Object child = object.opt(keys.next());
            if (child instanceof JSONArray) {
                return (JSONArray) child;
            }
        }
        return new JSONArray();
    }

    public static String extractCourseId(JSONObject course) {
        return stringValue(firstExisting(course, "ID", "Id", "id", "CourseID", "courseId"));
    }

    public static String extractCourseName(JSONObject course) {
        String name = stringValue(firstExisting(
                course,
                "CourseName",
                "course_name",
                "Title",
                "title",
                "Name",
                "name"
        ));
        return isEmpty(name) ? "unknown" : cleanText(name);
    }

    private static String normalizeDeadline(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return "";
        }
        if (value instanceof Number) {
            return formatTimestamp(((Number) value).longValue());
        }

        String raw = cleanText(String.valueOf(value)).trim();
        if (raw.isEmpty()) {
            return "";
        }
        if (raw.matches("\\d+")) {
            try {
                return formatTimestamp(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        String parsed = parseKnownDate(raw);
        return parsed == null ? raw : parsed;
    }

    private static String formatTimestamp(long raw) {
        return outputFormat().format(new Date(timestampMillis(raw)));
    }

    private static long normalizeTimestamp(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return 0L;
        }
        if (value instanceof Number) {
            return timestampMillis(((Number) value).longValue());
        }

        String raw = cleanText(String.valueOf(value)).trim();
        if (raw.isEmpty()) {
            return 0L;
        }
        if (raw.matches("\\d+")) {
            try {
                return timestampMillis(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        Date parsed = parseKnownDateValue(raw);
        return parsed == null ? 0L : parsed.getTime();
    }

    private static long timestampMillis(long raw) {
        return raw < 10000000000L ? raw * 1000L : raw;
    }

    private static String parseKnownDate(String raw) {
        Date date = parseKnownDateValue(raw);
        return date == null ? null : outputFormat().format(date);
    }

    private static Date parseKnownDateValue(String raw) {
        String normalized = raw.replace('T', ' ');
        if (normalized.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2} \\d{1,2}:\\d{1,2}(:\\d{1,2})?.*")) {
            Date date = tryParse(normalized, false,
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm");
            if (date != null) {
                return date;
            }
        }

        boolean utc = raw.endsWith("Z");
        Date iso = tryParse(raw, utc,
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return iso;
    }

    private static Date tryParse(String raw, boolean utc, String... patterns) {
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                format.setLenient(false);
                if (utc) {
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                return format.parse(raw);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static SimpleDateFormat outputFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    private static Object firstExisting(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            if (object.has(key) && !object.isNull(key)) {
                return object.opt(key);
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String cleanText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("ÖrÖn", "\n")
                .replace("Ön", "\n")
                .replace("Ör", "\n")
                .replace("Ä", "[")
                .replace("Ü", "]")
                .replace("ä", "{")
                .replace("ü", "}")
                .replace("ß", "~")
                .replace("§", "@")
                .trim();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
