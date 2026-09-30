package com.jushi.demo.main.ui.imports;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.chaosuan.EasyHpcHomework;
import com.jushi.demo.main.chaosuan.EasyHpcHomeworkSyncer;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EasyHpcImportActivity extends BaseActivity {
    private static final String PREFS_NAME = "easyhpc_import";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LAST_SYNC_PREFIX = "last_sync_";
    private static final String EASYHPC_PACKAGE_NAME = "com.huawei.easyhpc";

    private EditText usernameInput;
    private EditText passwordInput;
    private Button syncButton;
    private Button importButton;
    private TextView statusText;
    private View previewScroll;
    private LinearLayout sourceContainer;
    private final List<SourcePreview> sourcePreviews = new ArrayList<>();
    private final Map<String, NotificationRule> existingBindings = new HashMap<>();
    private List<CourseBindingOptions.Option> courseOptions = new ArrayList<>();
    private long currentSyncStartedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easyhpc_import);

        usernameInput = findViewById(R.id.etEasyHpcUsername);
        passwordInput = findViewById(R.id.etEasyHpcPassword);
        syncButton = findViewById(R.id.btnSyncEasyHpc);
        importButton = findViewById(R.id.btnImportEasyHpc);
        statusText = findViewById(R.id.tvEasyHpcStatus);
        previewScroll = findViewById(R.id.svEasyHpcPreview);
        sourceContainer = findViewById(R.id.llEasyHpcSources);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        usernameInput.setText(prefs.getString(KEY_USERNAME, ""));
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        syncButton.setOnClickListener(v -> startSync());
        importButton.setOnClickListener(v -> importSelectedSources());
        loadCourseBindingData();
    }

    private void startSync() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (username.isEmpty() || password.trim().isEmpty()) {
            Toast.makeText(this, "请输入超算习堂账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_USERNAME, username)
                .apply();

        syncButton.setEnabled(false);
        importButton.setEnabled(false);
        previewScroll.setVisibility(View.GONE);
        sourceContainer.removeAllViews();
        statusText.setText("正在抓取超算习堂作业...");
        long syncStartedAt = System.currentTimeMillis();

        new Thread(() -> {
            try {
                List<EasyHpcHomework> homeworks =
                        new EasyHpcHomeworkSyncer(this).fetchHomeworks(username, password);
                runOnUiThread(() -> showFetchedHomeworks(homeworks, syncStartedAt));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("抓取失败：" + shortMessage(e));
                    syncButton.setEnabled(true);
                    importButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void showFetchedHomeworks(List<EasyHpcHomework> homeworks, long syncStartedAt) {
        loadCourseBindingData();
        currentSyncStartedAt = syncStartedAt;
        sourcePreviews.clear();
        sourcePreviews.addAll(groupBySource(homeworks));
        syncButton.setEnabled(true);
        int fetchedCount = homeworks == null ? 0 : homeworks.size();

        if (sourcePreviews.isEmpty()) {
            previewScroll.setVisibility(View.GONE);
            importButton.setVisibility(View.GONE);
            statusText.setText("已抓取 " + fetchedCount + " 条作业，没有发现上次同步后的新作业");
            return;
        }

        renderSourcePreviews();
        previewScroll.setVisibility(View.VISIBLE);
        importButton.setVisibility(View.VISIBLE);
        importButton.setEnabled(true);
        statusText.setText("已抓取 " + fetchedCount
                + " 条作业，本次可导入 " + countHomeworks(sourcePreviews)
                + " 条，来自 " + sourcePreviews.size() + " 个来源");
    }

    private List<SourcePreview> groupBySource(List<EasyHpcHomework> homeworks) {
        Map<String, SourcePreview> bySource = new LinkedHashMap<>();
        if (homeworks == null) {
            return new ArrayList<>();
        }

        for (EasyHpcHomework homework : homeworks) {
            if (homework == null) {
                continue;
            }
            String key = sourceKey(homework.courseId, homework.courseName);
            long lastSyncAt = getLastSyncAt(key);
            if (lastSyncAt > 0 && homework.createdAt > 0 && homework.createdAt <= lastSyncAt) {
                continue;
            }

            SourcePreview preview = bySource.get(key);
            if (preview == null) {
                preview = new SourcePreview();
                preview.sourceKey = key;
                preview.sourceCourseId = safe(homework.courseId);
                preview.sourceCourseName = sourceDisplayName(homework.courseId, homework.courseName);
                preview.lastSyncAt = lastSyncAt;
                applyExistingBinding(preview);
                bySource.put(key, preview);
            }
            preview.homeworks.add(homework);
        }
        return new ArrayList<>(bySource.values());
    }

    private void applyExistingBinding(SourcePreview preview) {
        NotificationRule rule = existingBindings.get(sourceDisplayName(
                preview.sourceCourseId,
                preview.sourceCourseName
        ));
        if (rule == null) {
            return;
        }
        preview.boundCourseId = rule.courseId;
        preview.boundCourseName = safe(rule.courseName);
    }

    private void renderSourcePreviews() {
        sourceContainer.removeAllViews();
        int textColor = resolveColorOnSurface();
        for (SourcePreview preview : sourcePreviews) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 12, 0, 12);

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(preview.sourceCourseName + "（" + preview.homeworks.size() + " 条作业）");
            checkBox.setTextColor(textColor);
            checkBox.setChecked(preview.selected);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> preview.selected = isChecked);
            row.addView(checkBox, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView taskPreview = new TextView(this);
            taskPreview.setText(buildTaskPreview(preview));
            taskPreview.setTextColor((textColor & 0x00FFFFFF) | 0x99000000);
            taskPreview.setTextSize(13f);
            taskPreview.setPadding(8, 2, 8, 4);
            row.addView(taskPreview, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            Button courseButton = new Button(this);
            courseButton.setAllCaps(false);
            courseButton.setTextSize(13f);
            updateCourseButtonText(courseButton, preview);
            courseButton.setOnClickListener(v -> showCoursePicker(preview, courseButton));
            row.addView(courseButton, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            sourceContainer.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    private String buildTaskPreview(SourcePreview preview) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, preview.homeworks.size());
        for (int i = 0; i < limit; i++) {
            EasyHpcHomework homework = preview.homeworks.get(i);
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- ").append(bestTitle(homework));
            if (!isEmpty(homework.deadline)) {
                builder.append(" / 截止：").append(homework.deadline.trim());
            }
        }
        if (preview.homeworks.size() > limit) {
            builder.append('\n').append("还有 ")
                    .append(preview.homeworks.size() - limit)
                    .append(" 条...");
        }
        return builder.toString();
    }

    private void showCoursePicker(SourcePreview preview, Button courseButton) {
        CharSequence[] items = CourseBindingOptions.toDialogItems(courseOptions);
        int checked = CourseBindingOptions.findDialogIndex(
                courseOptions,
                preview.boundCourseId,
                preview.boundCourseName
        );
        new AlertDialog.Builder(this)
                .setTitle("绑定课程")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    CourseBindingOptions.Option option =
                            CourseBindingOptions.optionAtDialogIndex(courseOptions, which);
                    if (option == null) {
                        preview.boundCourseId = CourseBindingOptions.NO_COURSE_ID;
                        preview.boundCourseName = "";
                    } else {
                        preview.boundCourseId = option.courseId;
                        preview.boundCourseName = option.courseName;
                    }
                    updateCourseButtonText(courseButton, preview);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateCourseButtonText(Button button, SourcePreview preview) {
        button.setText("绑定课程: "
                + CourseBindingOptions.displayCourseName(preview.boundCourseName));
    }

    private void importSelectedSources() {
        List<EasyHpcHomeworkSyncer.ImportSource> sources = buildSelectedImportSources();
        if (sources.isEmpty()) {
            Toast.makeText(this, "请至少选择一个超算习堂来源", Toast.LENGTH_SHORT).show();
            return;
        }

        syncButton.setEnabled(false);
        importButton.setEnabled(false);
        statusText.setText("正在导入已选作业...");

        new Thread(() -> {
            try {
                EasyHpcHomeworkSyncer.SyncResult result =
                        new EasyHpcHomeworkSyncer(this).importToDatabase(sources);
                runOnUiThread(() -> {
                    saveLastSyncForSelectedSources();
                    statusText.setText("导入完成：选择 " + result.selectedSourceCount
                            + " 个来源、" + result.fetchedCount
                            + " 条作业，新增 " + result.importedCount
                            + " 条，跳过重复 " + result.skippedCount + " 条");
                    Toast.makeText(this, "超算习堂导入完成", Toast.LENGTH_SHORT).show();
                    syncButton.setEnabled(true);
                    importButton.setEnabled(true);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("导入失败：" + shortMessage(e));
                    syncButton.setEnabled(true);
                    importButton.setEnabled(true);
                });
            }
        }).start();
    }

    private List<EasyHpcHomeworkSyncer.ImportSource> buildSelectedImportSources() {
        List<EasyHpcHomeworkSyncer.ImportSource> sources = new ArrayList<>();
        for (SourcePreview preview : sourcePreviews) {
            if (!preview.selected) {
                continue;
            }
            EasyHpcHomeworkSyncer.ImportSource source = new EasyHpcHomeworkSyncer.ImportSource();
            source.sourceCourseId = preview.sourceCourseId;
            source.sourceCourseName = preview.sourceCourseName;
            source.boundCourseId = preview.boundCourseId;
            source.boundCourseName = preview.boundCourseName;
            source.selected = true;
            source.homeworks.addAll(preview.homeworks);
            sources.add(source);
        }
        return sources;
    }

    private void saveLastSyncForSelectedSources() {
        long syncStartedAt = currentSyncStartedAt > 0 ? currentSyncStartedAt : System.currentTimeMillis();
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        for (SourcePreview preview : sourcePreviews) {
            if (!preview.selected) {
                continue;
            }
            String key = isEmpty(preview.sourceKey)
                    ? sourceKey(preview.sourceCourseId, preview.sourceCourseName)
                    : preview.sourceKey;
            editor.putLong(lastSyncPreferenceKey(key), syncStartedAt);
        }
        editor.apply();
    }

    private void loadCourseBindingData() {
        DBManager db = new DBManager(this);
        try {
            List<Course> courses = db.getAllCourses();
            courseOptions = CourseBindingOptions.fromCourses(courses);
            existingBindings.clear();
            for (NotificationRule rule : db.getEnabledNotificationRules()) {
                if (rule != null && EASYHPC_PACKAGE_NAME.equals(rule.packageName)) {
                    existingBindings.put(safe(rule.groupName), rule);
                }
            }
        } finally {
            db.close();
        }
    }

    private static int countHomeworks(List<SourcePreview> previews) {
        int count = 0;
        for (SourcePreview preview : previews) {
            count += preview.homeworks.size();
        }
        return count;
    }

    private int resolveColorOnSurface() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        return typedValue.data;
    }

    private static String shortMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 120 ? message.substring(0, 120) + "..." : message;
    }

    private static String bestTitle(EasyHpcHomework homework) {
        if (homework == null) {
            return "超算习堂作业";
        }
        if (!isEmpty(homework.taskTitle)) {
            return homework.taskTitle.trim();
        }
        if (!isEmpty(homework.description)) {
            return homework.description.trim();
        }
        return "超算习堂作业";
    }

    private static String sourceKey(String courseId, String courseName) {
        if (!isEmpty(courseId)) {
            return "id:" + courseId.trim();
        }
        return "name:" + sourceDisplayName(courseId, courseName);
    }

    private long getLastSyncAt(String sourceKey) {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getLong(lastSyncPreferenceKey(sourceKey), 0L);
    }

    private static String lastSyncPreferenceKey(String sourceKey) {
        return KEY_LAST_SYNC_PREFIX + safe(sourceKey);
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

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class SourcePreview {
        String sourceKey;
        String sourceCourseId;
        String sourceCourseName;
        long lastSyncAt;
        int boundCourseId = CourseBindingOptions.NO_COURSE_ID;
        String boundCourseName = "";
        boolean selected = true;
        final List<EasyHpcHomework> homeworks = new ArrayList<>();
    }
}
