package com.jushi.demo.main.ui.imports;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

import java.util.ArrayList;
import java.util.List;

public class SelectSourceActivity extends BaseActivity {
    public static final String PREFS_NAME = "jushi_demo_prefs";
    public static final String KEY_SELECTED_SOURCE_IDS = "selected_source_ids";
    public static final String KEY_SOURCE_TARGETS_FORMATTED = "source_targets_formatted";
    public static final String EXTRA_SELECTED_SOURCE_IDS = "extra_selected_source_ids";
    public static final String EXTRA_SELECTED_SOURCE_NAMES = "extra_selected_source_names";

    private final List<SourceItem> sourceItems = new ArrayList<>();
    private TextView tvSelectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_source);

        RecyclerView recyclerView = findViewById(R.id.rvSources);
        Button btnSave = findViewById(R.id.btnSaveSources);
        tvSelectionResult = findViewById(R.id.tvSelectionResult);

        buildDefaultSources();
        recoverSelections();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new SourceAdapter(sourceItems));

        btnSave.setOnClickListener(v -> goToTargetConfigPage());
        updateResultText();
    }

    private void buildDefaultSources() {
        sourceItems.clear();
        sourceItems.add(new SourceItem("wechat", "微信", false));
        sourceItems.add(new SourceItem("wecom", "企业微信", false));
        sourceItems.add(new SourceItem("qq", "QQ", false));
        sourceItems.add(new SourceItem("superstarlearn", "学习通", false));
        sourceItems.add(new SourceItem("easyhpc", "超算习堂", false));
        sourceItems.add(new SourceItem("yuketang", "雨课堂", false));
    }

    private void recoverSelections() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedIds = sp.getString(KEY_SELECTED_SOURCE_IDS, "");
        if (selectedIds == null || selectedIds.trim().isEmpty()) {
            return;
        }
        for (SourceItem item : sourceItems) {
            item.setSelected(selectedIds.contains(item.getId() + ","));
        }
    }

    private void goToTargetConfigPage() {
        StringBuilder ids = new StringBuilder();
        StringBuilder names = new StringBuilder();

        for (SourceItem item : sourceItems) {
            if (item.isSelected()) {
                ids.append(item.getId()).append(",");
                names.append(item.getName()).append(",");
            }
        }

        if (ids.length() == 0) {
            Toast.makeText(this, "请至少选择一个消息源", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_SOURCE_IDS, ids.toString())
                .apply();

        Intent intent = new Intent(this, ConfigureTargetsActivity.class);
        intent.putExtra(EXTRA_SELECTED_SOURCE_IDS, ids.toString());
        intent.putExtra(EXTRA_SELECTED_SOURCE_NAMES, names.toString());
        startActivity(intent);
    }

    private void updateResultText() {
        StringBuilder names = new StringBuilder();
        for (SourceItem item : sourceItems) {
            if (item.isSelected()) {
                names.append(item.getName()).append("、");
            }
        }
        if (names.length() == 0) {
            tvSelectionResult.setText("当前未选择消息源");
        } else {
            names.deleteCharAt(names.length() - 1);
            tvSelectionResult.setText("当前已选: " + names);
        }
    }
}
