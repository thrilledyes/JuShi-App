package com.jushi.demo.main.ui.imports;

import com.jushi.demo.main.data.entity.Course;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CourseBindingOptions {
    static final int NO_COURSE_ID = 0;

    private CourseBindingOptions() {
    }

    static List<Option> fromCourses(List<Course> courses) {
        Map<String, Option> byName = new LinkedHashMap<>();
        if (courses == null) {
            return new ArrayList<>();
        }

        for (Course course : courses) {
            if (course == null || isEmpty(course.getCourseName())) {
                continue;
            }
            String courseName = course.getCourseName().trim();
            String key = normalizeName(courseName);
            if (!byName.containsKey(key)) {
                byName.put(key, new Option(course.getId(), courseName, courseName));
            }
        }
        return new ArrayList<>(byName.values());
    }

    static CharSequence[] toDialogItems(List<Option> options) {
        int optionCount = options == null ? 0 : options.size();
        CharSequence[] items = new CharSequence[optionCount + 1];
        items[0] = "不绑定课程";
        for (int i = 0; i < optionCount; i++) {
            items[i + 1] = options.get(i).label;
        }
        return items;
    }

    static Option optionAtDialogIndex(List<Option> options, int dialogIndex) {
        if (dialogIndex <= 0 || options == null || dialogIndex > options.size()) {
            return null;
        }
        return options.get(dialogIndex - 1);
    }

    static int findDialogIndex(List<Option> options, int courseId, String courseName) {
        if (options == null || options.isEmpty()) {
            return 0;
        }

        if (courseId > 0) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).courseId == courseId) {
                    return i + 1;
                }
            }
        }

        String normalizedCourseName = normalizeName(courseName);
        if (!normalizedCourseName.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                if (normalizeName(options.get(i).courseName).equals(normalizedCourseName)) {
                    return i + 1;
                }
            }
        }
        return 0;
    }

    static String displayCourseName(String courseName) {
        return isEmpty(courseName) ? "未绑定课程" : courseName.trim();
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s_\\-—:：,，.。()（）\\[\\]【】]", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class Option {
        final int courseId;
        final String courseName;
        final String label;

        Option(int courseId, String courseName, String label) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.label = label;
        }
    }
}
