package com.jushi.demo.main.chaosuan;

import android.content.Context;

import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.RawMessage;
import com.jushi.demo.main.database.model.SourceType;

import java.util.List;

public class EasyHpcHomeworkSyncer {
    private static final String PACKAGE_NAME = "com.huawei.easyhpc";

    private final Context context;
    private final EasyHpcHomeworkClient client;

    public EasyHpcHomeworkSyncer(Context context) {
        this(context, new EasyHpcHomeworkClient());
    }

    EasyHpcHomeworkSyncer(Context context, EasyHpcHomeworkClient client) {
        this.context = context.getApplicationContext();
        this.client = client;
    }

    public SyncResult syncToDatabase(String username, String password) throws Exception {
        List<EasyHpcHomework> homeworks = client.fetchAllHomeworks(username, password);
        SyncResult result = new SyncResult();
        result.fetchedCount = homeworks.size();

        DBManager dbManager = new DBManager(context);
        try {
            for (EasyHpcHomework homework : homeworks) {
                long rawId = dbManager.insertRawMessage(toRawMessage(homework));
                if (rawId == -1) {
                    result.skippedCount++;
                    continue;
                }

                long normalizedId = dbManager.insertNormalizedRecord(
                        safe(homework.courseName),
                        bestTitle(homework),
                        safe(homework.description),
                        safe(homework.deadline),
                        homework.confidence
                );
                if (normalizedId != -1) {
                    dbManager.linkRawToNormalized(rawId, normalizedId);
                }

                dbManager.insertTodoMessage(
                        sessionId(homework.courseName),
                        safe(homework.courseName),
                        "ddl",
                        "bot",
                        todoTitle(homework.courseName),
                        bestTitle(homework),
                        safe(homework.deadline),
                        rawId
                );
                result.importedCount++;
            }
        } finally {
            dbManager.close();
        }

        return result;
    }

    private RawMessage toRawMessage(EasyHpcHomework homework) {
        RawMessage message = new RawMessage();
        message.sessionId = sessionId(homework.courseName);
        message.source = SourceType.EASYHPC;
        message.sender = safe(homework.courseName);
        message.title = bestTitle(homework);
        message.content = buildContent(homework);
        message.rawPayload = safe(homework.rawPayload);
        message.timestamp = System.currentTimeMillis();
        message.messageHash = homework.stableHash();
        return message;
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
    }
}
