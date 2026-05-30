package com.jushi.demo.main.ui.imports;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ConfigureTargetsActivity extends BaseActivity {
    private static final String CHAT_HINT = "每行填写一个群聊/课程名称";

    private LinearLayout container;
    private TextView tvPreview;
    private String[] sourcePackages;
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
        btnSave.setOnClickListener(v -> saveTargetsAsRulesJson());
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
        int surfaceColor = resolveColorOnSurface();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < sourcePackages.length && i < sourceNames.length; i++) {
            String sourceName = sourceNames[i];

            TextView title = new TextView(this);
            title.setText(sourceName);
            title.setTextSize(17f);
            title.setTextColor(surfaceColor);
            title.setPadding(0, 20, 0, 8);
            container.addView(title);

            EditText input = new EditText(this);
            input.setId(buildInputId(i));
            input.setMinLines(3);
            input.setMaxLines(6);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(8, density));
            int strokeColor = (surfaceColor & 0x00FFFFFF) | 0x66000000;
            bg.setStroke(dpToPx(1, density), strokeColor);
            bg.setColor(android.graphics.Color.TRANSPARENT);
            input.setBackground(bg);

            input.setPadding(24, 20, 24, 20);
            input.setTextColor(surfaceColor);
            input.setHintTextColor((surfaceColor & 0x00FFFFFF) | 0x99000000);
            input.setHint(CHAT_HINT);
            container.addView(input);
        }
    }

    private int buildInputId(int index) {
        return 2000 + index;
    }

    private int resolveColorOnSurface() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        return typedValue.data;
    }

    private static int dpToPx(int dp, float density) {
        return (int) (dp * density + 0.5f);
    }

    private void saveTargetsAsRulesJson() {
        JSONArray sources = new JSONArray();
        StringBuilder preview = new StringBuilder();

        for (int i = 0; i < sourcePackages.length && i < sourceNames.length; i++) {
            String packageName = sourcePackages[i];
            String sourceName = sourceNames[i];
            EditText input = findViewById(buildInputId(i));
            if (input == null) {
                continue;
            }

            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("packageName", packageName);
                    item.put("groupName", "");
                    sources.put(item);
                } catch (JSONException e) {
                    Toast.makeText(this, "生成规则失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                preview.append(sourceName).append(" -> ").append(packageName).append(" (全部)\n");
                continue;
            }

            String[] lines = raw.split("\\n");
            for (String line : lines) {
                String groupName = line.trim();
                if (groupName.isEmpty()) {
                    continue;
                }
                try {
                    JSONObject item = new JSONObject();
                    item.put("packageName", packageName);
                    item.put("groupName", groupName);
                    sources.put(item);
                } catch (JSONException e) {
                    Toast.makeText(this, "生成规则失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                preview.append(sourceName).append(" -> ").append(packageName)
                        .append(" / ").append(groupName).append('\n');
            }
        }

        if (sources.length() == 0) {
            Toast.makeText(this, "请至少填写一个监听对象", Toast.LENGTH_SHORT).show();
            return;
        }

        final String json;
        try {
            JSONObject root = new JSONObject();
            root.put("sources", sources);
            json = root.toString();
        } catch (JSONException e) {
            Toast.makeText(this, "生成规则失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(SelectSourceActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(SelectSourceActivity.KEY_NOTIFICATION_RULES_JSON, json)
                .apply();

        try {
            File file = new File(getFilesDir(), "notification_rules.json");
            try (FileOutputStream fos = new FileOutputStream(file, false)) {
                fos.write(json.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Toast.makeText(this, "保存规则文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        tvPreview.setText(preview.toString().trim());
        Toast.makeText(this, "监听规则已保存", Toast.LENGTH_SHORT).show();
    }
}
