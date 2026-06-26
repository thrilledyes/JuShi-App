package com.jushi.demo.main.chaosuan;

public class EasyHpcHomework {
    public String courseId;
    public String courseName;
    public String homeworkId;
    public String taskTitle;
    public String description;
    public String deadline;
    public double confidence;
    public String rawPayload;

    public String stableHash() {
        String taskKey = isEmpty(homeworkId) ? safe(taskTitle) + "|" + safe(deadline) : homeworkId;
        return "easyhpc|" + safe(courseId) + "|" + safe(courseName) + "|" + taskKey;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
