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
    public static final String KEY_SELECTED_SOURCE_PACKAGES = "selected_source_packages";
    public static final String EXTRA_SELECTED_SOURCE_PACKAGES = "extra_selected_source_packages";
    public static final String EXTRA_SELECTED_SOURCE_NAMES = "extra_selected_source_names";
    private static final int REQUEST_CONFIGURE_TARGETS = 1001;

    private final List<SourceItem> sourceItems = new ArrayList<>();
    private TextView tvSelectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_source);

        RecyclerView recyclerView = findViewById(R.id.rvSources);
        Button btnSave = findViewById(R.id.btnSaveSources);
        Button btnPreviewEdit = findViewById(R.id.btnPreviewEditSources);
        tvSelectionResult = findViewById(R.id.tvSelectionResult);

        buildDefaultSources();
        recoverSelections();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new SourceAdapter(sourceItems));

        btnSave.setOnClickListener(v -> goToTargetConfigPage());
        btnPreviewEdit.setOnClickListener(v ->
                startActivity(new Intent(this, PreviewEditSourcesActivity.class)));
        updateResultText();
    }

    private void buildDefaultSources() {
        sourceItems.clear();
        sourceItems.add(new SourceItem("com.tencent.mm", "微信", false));
        sourceItems.add(new SourceItem("com.tencent.wework", "企业微信", false));
        sourceItems.add(new SourceItem("com.tencent.mobileqq", "QQ", false));
        sourceItems.add(new SourceItem("com.chaoxing.mobile", "学习通", false));
        sourceItems.add(new SourceItem("com.huawei.easyhpc", "超算习堂", false));
        sourceItems.add(new SourceItem("com.yuketang.app", "雨课堂", false));
    }

    private void recoverSelections() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedPackages = sp.getString(KEY_SELECTED_SOURCE_PACKAGES, "");
        if (selectedPackages == null || selectedPackages.trim().isEmpty()) {
            return;
        }
        for (SourceItem item : sourceItems) {
            item.setSelected(selectedPackages.contains(item.getPackageName() + ","));
        }
    }

    private void goToTargetConfigPage() {
        StringBuilder packages = new StringBuilder();
        StringBuilder names = new StringBuilder();

        for (SourceItem item : sourceItems) {
            if (item.isSelected()) {
                packages.append(item.getPackageName()).append(",");
                names.append(item.getName()).append(",");
            }
        }

        if (packages.length() == 0) {
            Toast.makeText(this, "请至少选择一个消息源", Toast.LENGTH_SHORT).show();
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_SOURCE_PACKAGES, packages.toString())
                .apply();

        Intent intent = new Intent(this, ConfigureTargetsActivity.class);
        intent.putExtra(EXTRA_SELECTED_SOURCE_PACKAGES, packages.toString());
        intent.putExtra(EXTRA_SELECTED_SOURCE_NAMES, names.toString());
        startActivityForResult(intent, REQUEST_CONFIGURE_TARGETS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIGURE_TARGETS && resultCode == RESULT_OK) {
            finish();
        }
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
