package com.jushi.demo.main.ui.imports;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

public class ConfigureTargetsActivity extends BaseActivity {
    private static final String CHAT_HINT = "每行填写一个群聊名称";
    private static final String COURSE_HINT = "每行填写一个课程名称";

    private LinearLayout container;
    private TextView tvPreview;
    private String[] sourceIds;
    private String[] sourceNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_targets);

        container = findViewById(R.id.llSourceTargetInputs);
        tvPreview = findViewById(R.id.tvTargetsPreview);
        Button btnSave = findViewById(R.id.btnSaveTargets);

        parseIntentData();
        buildInputBlocks();
        btnSave.setOnClickListener(v -> saveTargets());
    }

    private void parseIntentData() {
        String idsRaw = getIntent().getStringExtra(SelectSourceActivity.EXTRA_SELECTED_SOURCE_IDS);
        String namesRaw = getIntent().getStringExtra(SelectSourceActivity.EXTRA_SELECTED_SOURCE_NAMES);
        sourceIds = splitCsv(idsRaw);
        sourceNames = splitCsv(namesRaw);
    }

    private String[] splitCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new String[0];
        }
        String[] arr = csv.split(",");
        int validCount = 0;
        for (String item : arr) {
            if (!item.trim().isEmpty()) {
                validCount++;
            }
        }
        String[] out = new String[validCount];
        int index = 0;
        for (String item : arr) {
            String value = item.trim();
            if (!value.isEmpty()) {
                out[index++] = value;
            }
        }
        return out;
    }

    private void buildInputBlocks() {
        container.removeAllViews();
        for (int i = 0; i < sourceIds.length && i < sourceNames.length; i++) {
            String sourceId = sourceIds[i];
            String sourceName = sourceNames[i];

            TextView title = new TextView(this);
            title.setText(sourceName);
            title.setTextSize(17f);
            title.setTextColor(resolveColorOnSurface());
            title.setPadding(0, 20, 0, 8);
            container.addView(title);

            EditText input = new EditText(this);
            input.setId(buildInputId(i));
            input.setMinLines(3);
            input.setMaxLines(6);
            input.setBackgroundResource(android.R.drawable.edit_text);
            input.setPadding(24, 20, 24, 20);
            input.setHint(isChatSource(sourceId) ? CHAT_HINT : COURSE_HINT);
            container.addView(input);
        }
    }

    private int buildInputId(int index) {
        return 2000 + index;
    }

    private boolean isChatSource(String sourceId) {
        return "wechat".equals(sourceId) || "wecom".equals(sourceId) || "qq".equals(sourceId);
    }

    private int resolveColorOnSurface() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        return typedValue.data;
    }

    private void saveTargets() {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < sourceIds.length && i < sourceNames.length; i++) {
            EditText input = findViewById(buildInputId(i));
            if (input == null) {
                continue;
            }
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                continue;
            }
            String[] lines = raw.split("\\n");
            for (String line : lines) {
                String value = line.trim();
                if (value.isEmpty()) {
                    continue;
                }
                formatted.append(sourceNames[i]).append("群聊或课程名称：").append(value).append("\n");
            }
        }

        if (formatted.length() == 0) {
            Toast.makeText(this, "请至少填写一个监听对象", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(SelectSourceActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SelectSourceActivity.KEY_SOURCE_TARGETS_FORMATTED, formatted.toString())
                .apply();

        tvPreview.setText(formatted.toString().trim());
        Toast.makeText(this, "监听对象已保存", Toast.LENGTH_SHORT).show();
    }
}
