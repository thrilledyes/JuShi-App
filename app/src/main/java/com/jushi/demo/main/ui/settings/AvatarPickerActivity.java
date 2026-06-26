package com.jushi.demo.main.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AvatarPickerActivity extends BaseActivity {
    public static final String EXTRA_AVATAR_PATH = "extra_avatar_path";

    private static final int REQUEST_CAMERA_PERMISSION = 4001;
    private static final int REQUEST_TAKE_PHOTO = 4002;
    private static final int REQUEST_CHOOSE_GALLERY = 4003;

    private Uri pendingCameraUri;
    private File pendingCameraFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_picker);

        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnChooseGallery = findViewById(R.id.btnChooseGallery);

        btnTakePhoto.setOnClickListener(v -> startCameraWithPermission());
        btnChooseGallery.setOnClickListener(v -> openGallery());
    }

    private void startCameraWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        openCamera();
    }

    private void openCamera() {
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, "无法创建相机缓存目录", Toast.LENGTH_SHORT).show();
                return;
            }

            pendingCameraFile = new File(dir, "avatar_camera.jpg");
            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pendingCameraFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开相机: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CHOOSE_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
            return;
        }
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_TAKE_PHOTO && pendingCameraFile != null && pendingCameraFile.exists()) {
            saveAvatarFromFile(pendingCameraFile);
            return;
        }

        if (requestCode == REQUEST_CHOOSE_GALLERY && data != null && data.getData() != null) {
            saveAvatarFromUri(data.getData());
        }
    }

    private void saveAvatarFromFile(File source) {
        try (InputStream inputStream = new FileInputStream(source)) {
            File avatarFile = copyToProfileAvatar(inputStream);
            finishWithAvatar(avatarFile);
        } catch (Exception e) {
            Toast.makeText(this, "保存头像失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAvatarFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }
            File avatarFile = copyToProfileAvatar(inputStream);
            finishWithAvatar(avatarFile);
        } catch (Exception e) {
            Toast.makeText(this, "保存头像失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File copyToProfileAvatar(InputStream inputStream) throws Exception {
        File avatarFile = new File(getFilesDir(), "profile_avatar.jpg");
        try (OutputStream outputStream = new FileOutputStream(avatarFile, false)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
        return avatarFile;
    }

    private void finishWithAvatar(File avatarFile) {
        Intent result = new Intent();
        result.putExtra(EXTRA_AVATAR_PATH, avatarFile.getAbsolutePath());
        setResult(RESULT_OK, result);
        finish();
    }
}
