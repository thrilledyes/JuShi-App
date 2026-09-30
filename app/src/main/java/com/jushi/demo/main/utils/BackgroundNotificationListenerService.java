package com.jushi.demo.main.utils;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.jushi.demo.main.ai.UIEPredictor;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;
import com.jushi.demo.main.database.model.RawMessage;
import com.jushi.demo.main.database.model.SourceType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    private ExecutorService aiTaskExecutor;
    private UIEPredictor sharedPredictor;

    @Override
    public void onCreate() {
        super.onCreate();
        aiTaskExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "Notification listener connected");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w(TAG, "Notification listener disconnected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        MessageModel message = NotificationMessageParser.parse(sbn);
        if (message == null) {
            return;
        }
        RuleMatch ruleMatch = matchNotificationRule(message);
        if (!shouldAccept(message, ruleMatch)) {
            return;
        }
        enqueueAiExtractionTask(message, ruleMatch.rule);
    }

    private void enqueueAiExtractionTask(MessageModel message, NotificationRule matchedRule) {
        aiTaskExecutor.execute(() -> {
            DBManager dbManager = null;
            try {
                dbManager = new DBManager(getApplicationContext());
                long rawId = dbManager.insertRawMessage(toRawMessage(message));
                Log.i(TAG, "原始消息已存入，rawId: " + rawId);

                UIEPredictor predictor = getSharedPredictor();
                if (predictor == null || !predictor.isInitialized()) {
                    Log.e(TAG, "AI 结果未入库: 模型初始化失败");
                    return;
                }

                String rawContent = message.getContent();
                String modelContent = buildModelContent(rawContent);
                String senderGroup = message.getSender();
                String boundCourseName = matchedRule == null ? "" : matchedRule.courseName;
                String taskName = predictor.extractInfo(modelContent, "任务");
                String rawDeadline = predictor.extractInfo(modelContent, "时间");
                String normalizedDeadline = TimeNormalizer.normalize(
                        getApplicationContext(),
                        rawDeadline,
                        senderGroup,
                        boundCourseName
                );
                Log.i(TAG, "AI 抽取结果: taskName=" + taskName
                        + ", rawDeadline=" + rawDeadline
                        + ", normalizedDeadline=" + normalizedDeadline
                        + ", boundCourse=" + boundCourseName
                        + ", modelContent=" + modelContent
                        + ", rawContent=" + rawContent);

                if (isValidTask(taskName) && normalizedDeadline != null) {
                    String recordCourseName = isEmpty(boundCourseName) ? senderGroup : boundCourseName;
                    String todoTitle = buildAutoTodoTitle(boundCourseName, senderGroup);
                    long normalizedId = dbManager.insertNormalizedRecord(
                            recordCourseName,
                            taskName,
                            rawContent,
                            normalizedDeadline,
                            1.0
                    );
                    if (rawId != -1 && normalizedId != -1) {
                        dbManager.linkRawToNormalized(rawId, normalizedId);
                        dbManager.insertTodoMessage(
                                message.getSessionId(),
                                senderGroup,
                                "ddl",
                                "bot",
                                todoTitle,
                                taskName,
                                normalizedDeadline,
                                rawId
                        );
                        Log.d(TAG, "AI 待办已写入并关联成功! NormalizedId: " + normalizedId);
                    }
                } else {
                    Log.i(TAG, "AI 结果未入库: 任务或时间未通过有效性检查");
                }
            } catch (Exception e) {
                Log.e(TAG, "AI 提取或存库异常", e);
            } finally {
                if (dbManager != null) {
                    dbManager.close();
                }
            }
        });
    }

    private UIEPredictor getSharedPredictor() {
        if (sharedPredictor == null || !sharedPredictor.isInitialized()) {
            if (sharedPredictor != null) {
                sharedPredictor.release();
            }
            sharedPredictor = new UIEPredictor();
            sharedPredictor.init(getApplicationContext());
        }
        return sharedPredictor;
    }

    private String buildModelContent(String rawContent) {
        if (rawContent == null) {
            return "";
        }

        String content = rawContent
                .replace('\u2005', ' ')
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        content = content.replaceAll("^\\[\\d+条]\\s*", "");
        content = content.replaceAll("^\\d+条\\s*", "");
        content = content.replaceAll("^@所有人\\s*", "");
        content = content.replaceAll("@所有人\\s*", "");

        int colonIndex = findSenderColon(content);
        if (colonIndex >= 0 && colonIndex + 1 < content.length()) {
            content = content.substring(colonIndex + 1).trim();
        }

        return content.isEmpty() ? rawContent.trim() : content;
    }

    private int findSenderColon(String content) {
        int chineseColon = content.indexOf('：');
        int asciiColon = content.indexOf(':');
        int colonIndex;
        if (chineseColon < 0) {
            colonIndex = asciiColon;
        } else if (asciiColon < 0) {
            colonIndex = chineseColon;
        } else {
            colonIndex = Math.min(chineseColon, asciiColon);
        }
        if (colonIndex < 0 || colonIndex > 24) {
            return -1;
        }
        return colonIndex;
    }

    private boolean shouldAccept(MessageModel message, RuleMatch ruleMatch) {
        if (ruleMatch == null || !ruleMatch.accepted) {
            return false;
        }

        String content = message.getContent();
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String lowerContent = content.toLowerCase();
        String[] keywords = {
                "作业", "实验", "lab", "ddl", "截止", "交", "提交",
                "考试", "测验", "期中", "期末", "习题", "课堂", "微云",
                "预习", "发布", "考勤", "查收", "上传", "逾期"
        };

        for (String keyword : keywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private RuleMatch matchNotificationRule(MessageModel message) {
        DBManager dbManager = new DBManager(getApplicationContext());
        List<NotificationRule> rules;
        try {
            rules = dbManager.getEnabledNotificationRules();
        } finally {
            dbManager.close();
        }

        if (rules.isEmpty()) {
            return RuleMatch.accepted(null);
        }

        String groupName = extractGroupNameFromSessionId(message.getSessionId());
        for (NotificationRule rule : rules) {
            boolean packageMatched = rule.packageName == null || rule.packageName.isEmpty()
                    || message.getSessionId().startsWith(rule.packageName + "_");
            boolean groupMatched = rule.groupName == null || rule.groupName.isEmpty()
                    || groupName.contains(rule.groupName);
            if (packageMatched && groupMatched) {
                return RuleMatch.accepted(rule);
            }
        }
        return RuleMatch.rejected();
    }

    private boolean isValidTask(String taskName) {
        return taskName != null
                && !taskName.trim().isEmpty()
                && !taskName.contains("未检测")
                && !taskName.contains("越界")
                && !"模型未初始化".equals(taskName);
    }

    private String extractGroupNameFromSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return "";
        int index = sessionId.indexOf('_');
        if (index < 0 || index + 1 >= sessionId.length()) return "";
        return sessionId.substring(index + 1);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String buildAutoTodoTitle(String boundCourseName, String groupName) {
        if (!isEmpty(boundCourseName)) {
            return boundCourseName.trim() + "课程任务";
        }
        String source = isEmpty(groupName) ? "群聊" : groupName.trim();
        return "来自" + source + "的待办";
    }

    private static class RuleMatch {
        final boolean accepted;
        final NotificationRule rule;

        private RuleMatch(boolean accepted, NotificationRule rule) {
            this.accepted = accepted;
            this.rule = rule;
        }

        static RuleMatch accepted(NotificationRule rule) {
            return new RuleMatch(true, rule);
        }

        static RuleMatch rejected() {
            return new RuleMatch(false, null);
        }
    }

    private RawMessage toRawMessage(MessageModel message) {
        RawMessage rawMessage = new RawMessage();
        rawMessage.sessionId = message.getSessionId();
        rawMessage.source = parseSourceType(message.getSource());
        rawMessage.sender = message.getSender();
        rawMessage.title = message.getTitle();
        rawMessage.content = message.getContent();
        rawMessage.rawPayload = message.getRawPayload();
        rawMessage.timestamp = message.getTimestamp();
        return rawMessage;
    }

    private SourceType parseSourceType(String source) {
        try {
            return SourceType.valueOf(source);
        } catch (Exception ignored) {
            return SourceType.ANDROID;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aiTaskExecutor != null) {
            aiTaskExecutor.shutdown();
        }
        if (sharedPredictor != null) {
            sharedPredictor.release();
            sharedPredictor = null;
        }
    }
}
