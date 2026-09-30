package com.jushi.demo.main.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

public class AuthoritySettingsActivity extends BaseActivity {
    private static final int REQUEST_READ_STORAGE = 1002;

    private Switch switchNotificationAccess;
    private Switch switchStorageAccess;
    private TextView tvNotificationPermissionStatus;
    private TextView tvStoragePermissionStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_authority);

        switchNotificationAccess = findViewById(R.id.switchNotificationAccess);
        switchStorageAccess = findViewById(R.id.switchStorageAccess);
        tvNotificationPermissionStatus = findViewById(R.id.tvNotificationPermissionStatus);
        tvStoragePermissionStatus = findViewById(R.id.tvStoragePermissionStatus);

        switchNotificationAccess.setOnClickListener(v -> openNotificationAccessSettings());
        switchStorageAccess.setOnClickListener(v -> openStorageAccessSettings());
        refreshPermissionState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
    }

    private void refreshPermissionState() {
        boolean notificationEnabled = isNotificationListenerEnabled();
        switchNotificationAccess.setChecked(notificationEnabled);
        tvNotificationPermissionStatus.setText(notificationEnabled
                ? "已开启，应用可以监听并筛选系统通知"
                : "未开启，消息监听模块不会收到系统通知");

        boolean storageEnabled = hasStorageAccess();
        switchStorageAccess.setChecked(storageEnabled);
        tvStoragePermissionStatus.setText(storageEnabled
                ? "已开启，可访问导入所需文件"
                : "未开启，可通过系统文件选择器导入，完整文件访问需额外授权");
    }

    private boolean isNotificationListenerEnabled() {
        String enabledListeners = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(getPackageName());
    }

    private void openNotificationAccessSettings() {
        Toast.makeText(this, "请在系统页面中允许本应用读取通知", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void openStorageAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(this, "请在系统页面中允许本应用管理文件", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception ignored) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_STORAGE
            );
        }
    }
}
