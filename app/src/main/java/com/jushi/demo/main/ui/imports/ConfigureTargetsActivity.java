package com.jushi.demo.main.ui.imports;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;

import java.util.ArrayList;
import java.util.List;

public class ConfigureTargetsActivity extends BaseActivity {
    private static final String TARGET_HINT = "群聊/课程名称，留空表示全部";

    private LinearLayout container;
    private TextView tvPreview;
    private String[] sourcePackages;
    private String[] sourceNames;
    private final List<SourceTargetBlock> blocks = new ArrayList<>();
    private final List<NotificationRule> existingRules = new ArrayList<>();
    private List<CourseBindingOptions.Option> courseOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_targets);

        container = findViewById(R.id.llSourceTargetInputs);
        tvPreview = findViewById(R.id.tvTargetsPreview);
        Button btnSave = findViewById(R.id.btnSaveTargets);

        parseIntentData();
        loadExistingData();
        buildInputBlocks();
        btnSave.setOnClickListener(v -> saveTargetsToDatabase());
    }

    private void parseIntentData() {
        String packagesRaw = getIntent().getStringExtra(SelectSourceActivity.EXTRA_SELECTED_SOURCE_PACKAGES);
        String namesRaw = getIntent().getStringExtra(SelectSourceActivity.EXTRA_SELECTED_SOURCE_NAMES);
        sourcePackages = splitCsv(packagesRaw);
        sourceNames = splitCsv(namesRaw);
    }

    private String[] splitCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new String[0];
        }
        String[] arr = csv.split(",");
        List<String> values = new ArrayList<>();
        for (String item : arr) {
            String value = item.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values.toArray(new String[0]);
    }

    private void buildInputBlocks() {
        container.removeAllViews();
        blocks.clear();
        int surfaceColor = resolveColorOnSurface();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < sourcePackages.length && i < sourceNames.length; i++) {
            SourceTargetBlock block = new SourceTargetBlock(sourcePackages[i], sourceNames[i]);
            blocks.add(block);

            TextView title = new TextView(this);
            title.setText(sourceNames[i]);
            title.setTextSize(17f);
            title.setTextColor(surfaceColor);
            title.setPadding(0, 20, 0, 8);
            container.addView(title);

            TextView hint = new TextView(this);
            hint.setText("每行配置一个监听对象，课程绑定用于识别“下次课/下节课”的具体时间。");
            hint.setTextSize(13f);
            hint.setTextColor(android.graphics.Color.GRAY);
            hint.setPadding(0, 0, 0, 6);
            container.addView(hint);

            block.rowsContainer = new LinearLayout(this);
            block.rowsContainer.setOrientation(LinearLayout.VERTICAL);
            container.addView(block.rowsContainer);

            List<NotificationRule> sourceRules = getExistingRulesForSource(sourcePackages[i]);
            if (sourceRules.isEmpty()) {
                addTargetRow(block, "", CourseBindingOptions.NO_COURSE_ID, "");
            } else {
                for (NotificationRule rule : sourceRules) {
                    addTargetRow(block, rule.groupName, rule.courseId, rule.courseName);
                }
            }

            Button addRow = new Button(this);
            addRow.setText("添加监听对象");
            addRow.setAllCaps(false);
            addRow.setOnClickListener(v -> addTargetRow(block, "", CourseBindingOptions.NO_COURSE_ID, ""));
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            addParams.setMargins(0, dpToPx(6, density), 0, dpToPx(6, density));
            container.addView(addRow, addParams);
        }
    }

    private int resolveColorOnSurface() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        return typedValue.data;
    }

    private static int dpToPx(int dp, float density) {
        return (int) (dp * density + 0.5f);
    }

    private void loadExistingData() {
        DBManager db = new DBManager(this);
        try {
            existingRules.clear();
            existingRules.addAll(db.getEnabledNotificationRules());
            List<Course> courses = db.getAllCourses();
            courseOptions = CourseBindingOptions.fromCourses(courses);
        } finally {
            db.close();
        }
    }

    private List<NotificationRule> getExistingRulesForSource(String packageName) {
        List<NotificationRule> matched = new ArrayList<>();
        for (NotificationRule rule : existingRules) {
            if (safe(rule.packageName).equals(safe(packageName))) {
                matched.add(rule);
            }
        }
        return matched;
    }

    private void addTargetRow(SourceTargetBlock block, String groupName, int courseId, String courseName) {
        int surfaceColor = resolveColorOnSurface();
        float density = getResources().getDisplayMetrics().density;

        TargetRow row = new TargetRow();
        row.courseId = courseId;
        row.courseName = safe(courseName);

        LinearLayout rowRoot = new LinearLayout(this);
        rowRoot.setOrientation(LinearLayout.VERTICAL);
        rowRoot.setPadding(0, dpToPx(8, density), 0, dpToPx(8, density));
        row.root = rowRoot;

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(safe(groupName));
        input.setTextColor(surfaceColor);
        input.setHintTextColor((surfaceColor & 0x00FFFFFF) | 0x99000000);
        input.setHint(TARGET_HINT);
        input.setPadding(24, 12, 24, 12);
        input.setBackground(buildInputBackground(surfaceColor, density));
        row.groupInput = input;
        rowRoot.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionParams.setMargins(0, dpToPx(6, density), 0, 0);

        Button courseButton = new Button(this);
        courseButton.setAllCaps(false);
        courseButton.setTextSize(13f);
        courseButton.setOnClickListener(v -> showCoursePicker(row));
        row.courseButton = courseButton;
        updateCourseButtonText(row);
        actionRow.addView(courseButton, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        Button removeButton = new Button(this);
        removeButton.setAllCaps(false);
        removeButton.setText("删除");
        removeButton.setOnClickListener(v -> {
            block.rows.remove(row);
            block.rowsContainer.removeView(row.root);
            if (block.rows.isEmpty()) {
                addTargetRow(block, "", CourseBindingOptions.NO_COURSE_ID, "");
            }
        });
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        removeParams.setMargins(dpToPx(8, density), 0, 0, 0);
        actionRow.addView(removeButton, removeParams);

        rowRoot.addView(actionRow, actionParams);
        block.rows.add(row);
        block.rowsContainer.addView(rowRoot);
    }

    private GradientDrawable buildInputBackground(int surfaceColor, float density) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(8, density));
        int strokeColor = (surfaceColor & 0x00FFFFFF) | 0x66000000;
        bg.setStroke(dpToPx(1, density), strokeColor);
        bg.setColor(android.graphics.Color.TRANSPARENT);
        return bg;
    }

    private void showCoursePicker(TargetRow row) {
        CharSequence[] items = CourseBindingOptions.toDialogItems(courseOptions);
        int checked = CourseBindingOptions.findDialogIndex(courseOptions, row.courseId, row.courseName);
        new AlertDialog.Builder(this)
                .setTitle("绑定课程")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    CourseBindingOptions.Option option = CourseBindingOptions.optionAtDialogIndex(courseOptions, which);
                    if (option == null) {
                        row.courseId = CourseBindingOptions.NO_COURSE_ID;
                        row.courseName = "";
                    } else {
                        row.courseId = option.courseId;
                        row.courseName = option.courseName;
                    }
                    updateCourseButtonText(row);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateCourseButtonText(TargetRow row) {
        row.courseButton.setText("绑定课程: " + CourseBindingOptions.displayCourseName(row.courseName));
    }

    private void saveTargetsToDatabase() {
        List<NotificationRule> rules = new ArrayList<>();
        StringBuilder preview = new StringBuilder();
        long now = System.currentTimeMillis();

        for (SourceTargetBlock block : blocks) {
            List<TargetRow> namedRows = new ArrayList<>();
            for (TargetRow row : block.rows) {
                if (!row.groupInput.getText().toString().trim().isEmpty()) {
                    namedRows.add(row);
                }
            }

            if (namedRows.isEmpty() && !block.rows.isEmpty()) {
                TargetRow row = block.rows.get(0);
                rules.add(buildRule(block.packageName, block.sourceName, "", row.courseId, row.courseName, now));
                appendPreview(preview, block.sourceName, block.packageName, "全部", row.courseName);
                continue;
            }

            for (TargetRow row : namedRows) {
                String groupName = row.groupInput.getText().toString().trim();
                rules.add(buildRule(block.packageName, block.sourceName, groupName, row.courseId, row.courseName, now));
                appendPreview(preview, block.sourceName, block.packageName, groupName, row.courseName);
            }
        }

        if (rules.isEmpty()) {
            Toast.makeText(this, "请至少填写一个监听对象", Toast.LENGTH_SHORT).show();
            return;
        }

        DBManager db = new DBManager(this);
        try {
            db.replaceNotificationRulesForPackages(sourcePackages, rules);
        } finally {
            db.close();
        }
        tvPreview.setText(preview.toString().trim());
        Toast.makeText(this, "监听规则已保存", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void appendPreview(
            StringBuilder preview,
            String sourceName,
            String packageName,
            String groupName,
            String courseName
    ) {
        preview.append(sourceName).append(" -> ").append(packageName)
                .append(" / ").append(groupName)
                .append(" / ").append(CourseBindingOptions.displayCourseName(courseName))
                .append('\n');
    }

    private NotificationRule buildRule(
            String packageName,
            String sourceName,
            String groupName,
            int courseId,
            String courseName,
            long now
    ) {
        NotificationRule rule = new NotificationRule();
        rule.packageName = packageName;
        rule.sourceName = sourceName;
        rule.groupName = groupName;
        rule.courseId = courseId;
        rule.courseName = safe(courseName);
        rule.enabled = true;
        rule.createdAt = now;
        rule.updatedAt = now;
        return rule;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static class SourceTargetBlock {
        final String packageName;
        final String sourceName;
        final List<TargetRow> rows = new ArrayList<>();
        LinearLayout rowsContainer;

        SourceTargetBlock(String packageName, String sourceName) {
            this.packageName = packageName;
            this.sourceName = sourceName;
        }
    }

    private static class TargetRow {
        View root;
        EditText groupInput;
        Button courseButton;
        int courseId;
        String courseName;
    }
}
