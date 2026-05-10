package com.jushi.demo.main;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.jushi.demo.main.ui.settings.UiContextWrapper;
import com.jushi.demo.main.ui.settings.UiPreferences;

public abstract class BaseActivity extends AppCompatActivity {
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(UiContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppCompatDelegate.setDefaultNightMode(UiPreferences.getThemeMode(this));
        applyGlobalTypography();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        applyGlobalTypography();
    }

    protected void applyGlobalTypography() {
        View root = getWindow().getDecorView().findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        boolean bold = UiPreferences.isFontBold(this);
        applyTypefaceRecursively(root, bold ? Typeface.BOLD : Typeface.NORMAL);
        uiHandler.post(() -> {
            View delayedRoot = getWindow().getDecorView().findViewById(android.R.id.content);
            if (delayedRoot == null) {
                return;
            }
            boolean delayedBold = UiPreferences.isFontBold(this);
            applyTypefaceRecursively(delayedRoot, delayedBold ? Typeface.BOLD : Typeface.NORMAL);
        });
    }

    private void applyTypefaceRecursively(View view, int style) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.setTypeface(tv.getTypeface(), style);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTypefaceRecursively(group.getChildAt(i), style);
            }
        }
    }
}
