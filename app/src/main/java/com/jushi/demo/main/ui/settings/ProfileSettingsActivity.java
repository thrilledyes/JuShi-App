package com.jushi.demo.main.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

import java.io.File;

public class ProfileSettingsActivity extends BaseActivity {
    private static final int REQUEST_AVATAR = 3001;
    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_AVATAR_PATH = "avatar_path";

    private ImageView imgAvatar;
    private EditText etNickname;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_profile);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        imgAvatar = findViewById(R.id.imgAvatar);
        etNickname = findViewById(R.id.etNickname);
        Button btnSave = findViewById(R.id.btnSaveProfile);

        loadProfile();
        imgAvatar.setOnClickListener(v -> startActivityForResult(
                new Intent(this, AvatarPickerActivity.class),
                REQUEST_AVATAR
        ));
        btnSave.setOnClickListener(v -> saveProfile());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_AVATAR || resultCode != RESULT_OK || data == null) {
            return;
        }

        String avatarPath = data.getStringExtra(AvatarPickerActivity.EXTRA_AVATAR_PATH);
        if (avatarPath == null || avatarPath.isEmpty()) {
            return;
        }

        prefs.edit().putString(KEY_AVATAR_PATH, avatarPath).apply();
        displayAvatar(avatarPath);
    }

    private void loadProfile() {
        etNickname.setText(prefs.getString(KEY_NICKNAME, ""));
        displayAvatar(prefs.getString(KEY_AVATAR_PATH, ""));
    }

    private void saveProfile() {
        prefs.edit()
                .putString(KEY_NICKNAME, etNickname.getText().toString().trim())
                .apply();
        Toast.makeText(this, "个人资料已保存", Toast.LENGTH_SHORT).show();
    }

    private void displayAvatar(String path) {
        if (path == null || path.isEmpty()) {
            imgAvatar.setImageResource(android.R.drawable.ic_menu_camera);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            imgAvatar.setImageResource(android.R.drawable.ic_menu_camera);
            return;
        }

        imgAvatar.setPadding(0, 0, 0, 0);
        imgAvatar.setImageURI(Uri.fromFile(file));
    }
}
