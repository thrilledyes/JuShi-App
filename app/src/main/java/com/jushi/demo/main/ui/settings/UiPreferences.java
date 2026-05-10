package com.jushi.demo.main.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class UiPreferences {
    public static final String PREFS_NAME = "jushi_ui_prefs";
    public static final String KEY_FONT_SCALE = "font_scale";
    public static final String KEY_FONT_BOLD = "font_bold";
    public static final String KEY_THEME_MODE = "theme_mode";

    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;

    private UiPreferences() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static float getFontScale(Context context) {
        return prefs(context).getFloat(KEY_FONT_SCALE, 1.0f);
    }

    public static boolean isFontBold(Context context) {
        return prefs(context).getBoolean(KEY_FONT_BOLD, false);
    }

    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public static void saveFontScale(Context context, float value) {
        prefs(context).edit().putFloat(KEY_FONT_SCALE, value).apply();
    }

    public static void saveFontBold(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_FONT_BOLD, value).apply();
    }

    public static void saveThemeMode(Context context, int value) {
        prefs(context).edit().putInt(KEY_THEME_MODE, value).apply();
    }
}
