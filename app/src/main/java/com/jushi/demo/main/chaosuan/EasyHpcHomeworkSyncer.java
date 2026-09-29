package com.jushi.demo.main.chaosuan;

import android.content.Context;

import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;
import com.jushi.demo.main.database.model.RawMessage;
import com.jushi.demo.main.database.model.SourceType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EasyHpcHomeworkSyncer {
    private static final String PACKAGE_NAME = "com.huawei.easyhpc";
    private static final String SOURCE_NAME = "超算习堂";

    private final Context context;
    private final EasyHpcHomeworkClient client;

    public EasyHpcHomeworkSyncer(Context context) {
        this(context, new EasyHpcHomeworkClient());
    }

    EasyHpcHomeworkSyncer(Context context, EasyHpcHomeworkClient client) {
        this.context = context.getApplicationContext();
        this.client = client;
    }

    public List<EasyHpcHomework> fetchHomeworks(String username, String password) throws Exception {
        return client.fetchAllHomeworks(username, password);
    }

    public SyncResult syncToDatabase(String username, String password) throws Exception {
        return importToDatabase(groupBySource(fetchHomeworks(username, password)));
    }

    public SyncResult importToDatabase(List<ImportSource> sources) {
        SyncResult result = new SyncResult();

        DBManager dbManager = new DBManager(context);
        try {
            List<NotificationRule> rules = new ArrayList<>();
            for (ImportSource source : safeSources(sources)) {
                if (!shouldImport(source)) {
                    continue;
                }
                result.selectedSourceCount++;
                rules.add(toNotificationRule(source));
            }
            dbManager.upsertNotificationRules(rules);

            for (ImportSource source : safeSources(sources)) {
                if (!shouldImport(source)) {
                    continue;
                }
                for (EasyHpcHomework homework : source.homeworks) {
                    if (homework == null) {
                        continue;
                    }
                    result.fetchedCount++;
                    importHomework(dbManager, source, homework, result);
                }
            }
        } finally {
            dbManager.close();
        }

        return result;
    }

    private void importHomework(
            DBManager dbManager,
            ImportSource source,
            EasyHpcHomework homework,
            SyncResult result
    ) {
        String courseName = effectiveCourseName(source, homework);
        long rawId = dbManager.insertRawMessage(toRawMessage(homework, courseName));
        if (rawId == -1) {
            result.skippedCount++;
            return;
        }

        long normalizedId = dbManager.insertNormalizedRecord(
                courseName,
                bestTitle(homework),
                safe(homework.description),
                safe(homework.deadline),
                homework.confidence
        );
        if (normalizedId != -1) {
            dbManager.linkRawToNormalized(rawId, normalizedId);
        }

        dbManager.insertTodoMessage(
                sessionId(courseName),
                courseName,
                "ddl",
                "bot",
                todoTitle(courseName),
                bestTitle(homework),
                safe(homework.deadline),
                rawId
        );
        result.importedCount++;
    }

    private RawMessage toRawMessage(EasyHpcHomework homework, String courseName) {
        RawMessage message = new RawMessage();
        message.sessionId = sessionId(courseName);
        message.source = SourceType.EASYHPC;
        message.sender = courseName;
        message.title = bestTitle(homework);
        message.content = buildContent(homework);
        message.rawPayload = safe(homework.rawPayload);
        message.timestamp = homework.createdAt > 0 ? homework.createdAt : System.currentTimeMillis();
        message.messageHash = homework.stableHash();
        return message;
    }

    private static List<ImportSource> groupBySource(List<EasyHpcHomework> homeworks) {
        Map<String, ImportSource> bySource = new LinkedHashMap<>();
        if (homeworks == null) {
            return new ArrayList<>();
        }

        for (EasyHpcHomework homework : homeworks) {
            if (homework == null) {
                continue;
            }
            String key = sourceKey(homework.courseId, homework.courseName);
            ImportSource source = bySource.get(key);
            if (source == null) {
                source = new ImportSource();
                source.sourceCourseId = safe(homework.courseId);
                source.sourceCourseName = sourceDisplayName(source.sourceCourseId, homework.courseName);
                bySource.put(key, source);
            }
            source.homeworks.add(homework);
        }
        return new ArrayList<>(bySource.values());
    }

    private static List<ImportSource> safeSources(List<ImportSource> sources) {
        return sources == null ? new ArrayList<>() : sources;
    }

    private static boolean shouldImport(ImportSource source) {
        return source != null && source.selected && source.homeworks != null && !source.homeworks.isEmpty();
    }

    private static NotificationRule toNotificationRule(ImportSource source) {
        NotificationRule rule = new NotificationRule();
        rule.packageName = PACKAGE_NAME;
        rule.sourceName = SOURCE_NAME;
        rule.groupName = sourceDisplayName(source);
        rule.courseId = source.boundCourseId;
        rule.courseName = safe(source.boundCourseName);
        rule.enabled = true;
        rule.createdAt = System.currentTimeMillis();
        rule.updatedAt = rule.createdAt;
        return rule;
    }

    private static String effectiveCourseName(ImportSource source, EasyHpcHomework homework) {
        if (source != null && !isEmpty(source.boundCourseName)) {
            return source.boundCourseName.trim();
        }
        if (source != null && !isEmpty(source.sourceCourseName)) {
            return source.sourceCourseName.trim();
        }
        if (homework != null && !isEmpty(homework.courseName)) {
            return homework.courseName.trim();
        }
        return "超算习堂课程";
    }

    private static String sourceDisplayName(ImportSource source) {
        if (source == null) {
            return "超算习堂课程";
        }
        return sourceDisplayName(source.sourceCourseId, source.sourceCourseName);
    }

    private static String sourceDisplayName(String courseId, String courseName) {
        if (!isEmpty(courseName)) {
            return courseName.trim();
        }
        if (!isEmpty(courseId)) {
            return "超算习堂课程 " + courseId.trim();
        }
        return "超算习堂课程";
    }

    private static String sourceKey(String courseId, String courseName) {
        if (!isEmpty(courseId)) {
            return "id:" + courseId.trim();
        }
        return "name:" + sourceDisplayName(courseId, courseName);
    }

    private static String buildContent(EasyHpcHomework homework) {
        StringBuilder builder = new StringBuilder();
        if (!isEmpty(homework.taskTitle)) {
            builder.append(homework.taskTitle.trim());
        }
        if (!isEmpty(homework.deadline)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("截止：").append(homework.deadline.trim());
        }
        if (!isEmpty(homework.description)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(homework.description.trim());
        }
        return builder.toString();
    }

    private static String bestTitle(EasyHpcHomework homework) {
        if (!isEmpty(homework.taskTitle)) {
            return homework.taskTitle.trim();
        }
        if (!isEmpty(homework.description)) {
            return homework.description.trim();
        }
        return "超算习堂作业";
    }

    private static String todoTitle(String courseName) {
        return isEmpty(courseName) ? "超算习堂课程任务" : courseName.trim() + "课程任务";
    }

    private static String sessionId(String courseName) {
        String name = isEmpty(courseName) ? "unknown" : courseName.trim();
        return PACKAGE_NAME + "_" + name;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class SyncResult {
        public int fetchedCount;
        public int importedCount;
        public int skippedCount;
        public int selectedSourceCount;
    }

    public static class ImportSource {
        public String sourceCourseId;
        public String sourceCourseName;
        public int boundCourseId;
        public String boundCourseName;
        public boolean selected = true;
        public final List<EasyHpcHomework> homeworks = new ArrayList<>();
    }
}
