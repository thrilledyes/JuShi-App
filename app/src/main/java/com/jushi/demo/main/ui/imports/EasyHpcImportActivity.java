package com.jushi.demo.main.ui.imports;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.chaosuan.EasyHpcHomeworkSyncer;

public class EasyHpcImportActivity extends BaseActivity {
    private static final String PREFS_NAME = "easyhpc_import";
    private static final String KEY_USERNAME = "username";

    private EditText usernameInput;
    private EditText passwordInput;
    private Button syncButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easyhpc_import);

        usernameInput = findViewById(R.id.etEasyHpcUsername);
        passwordInput = findViewById(R.id.etEasyHpcPassword);
        syncButton = findViewById(R.id.btnSyncEasyHpc);
        statusText = findViewById(R.id.tvEasyHpcStatus);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        usernameInput.setText(prefs.getString(KEY_USERNAME, ""));
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        syncButton.setOnClickListener(v -> startSync());
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
        statusText.setText("正在同步超算习堂作业...");

        new Thread(() -> {
            try {
                EasyHpcHomeworkSyncer.SyncResult result =
                        new EasyHpcHomeworkSyncer(this).syncToDatabase(username, password);
                runOnUiThread(() -> {
                    statusText.setText("同步完成：获取 " + result.fetchedCount
                            + " 条，新增 " + result.importedCount
                            + " 条，跳过重复 " + result.skippedCount + " 条");
                    Toast.makeText(this, "超算习堂同步完成", Toast.LENGTH_SHORT).show();
                    syncButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("同步失败：" + shortMessage(e));
                    syncButton.setEnabled(true);
                });
            }
        }).start();
    }

    private static String shortMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 120 ? message.substring(0, 120) + "..." : message;
    }
}
