package com.jushi.demo.main;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        afterContentViewSet();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        afterContentViewSet();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        afterContentViewSet();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppCompatDelegate.setDefaultNightMode(UiPreferences.getThemeMode(this));
        applySystemBarAppearance();
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

    private void afterContentViewSet() {
        applySystemBarAppearance();
        applySystemBarInsets();
    }

    private void applySystemBarAppearance() {
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) {
                return;
            }
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            int appearance = night ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mask |= WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                if (!night) {
                    appearance |= WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                }
            }
            controller.setSystemBarsAppearance(appearance, mask);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (night) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (night) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    private void applySystemBarInsets() {
        View content = getWindow().getDecorView().findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }

        ViewGroup contentGroup = (ViewGroup) content;
        if (contentGroup.getChildCount() == 0) {
            return;
        }

        View root = contentGroup.getChildAt(0);
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = 0;
            int top = 0;
            int right = 0;
            int bottom = 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
                );
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            view.setPadding(
                    initialLeft + left,
                    initialTop + top,
                    initialRight + right,
                    initialBottom + bottom
            );
            return insets;
        });
        root.requestApplyInsets();
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
