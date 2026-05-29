package com.jushi.demo.main.ui.settings;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;

public class GeneralSettingsActivity extends BaseActivity {
    private RadioGroup rgFontSize;
    private RadioGroup rgFontWeight;
    private RadioGroup rgTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_general);

        rgFontSize = findViewById(R.id.rgFontSize);
        rgFontWeight = findViewById(R.id.rgFontWeight);
        rgTheme = findViewById(R.id.rgTheme);

        bindCurrentValues();
        bindListeners();
    }

    private void bindCurrentValues() {
        float scale = UiPreferences.getFontScale(this);
        if (Math.abs(scale - UiPreferences.FONT_SCALE_SMALL) < 0.01f) {
            ((RadioButton) findViewById(R.id.rbFontSmall)).setChecked(true);
        } else if (Math.abs(scale - UiPreferences.FONT_SCALE_LARGE) < 0.01f) {
            ((RadioButton) findViewById(R.id.rbFontLarge)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbFontNormal)).setChecked(true);
        }

        boolean bold = UiPreferences.isFontBold(this);
        ((RadioButton) findViewById(bold ? R.id.rbWeightBold : R.id.rbWeightNormal)).setChecked(true);

        int theme = UiPreferences.getThemeMode(this);
        if (theme == UiPreferences.THEME_LIGHT) {
            ((RadioButton) findViewById(R.id.rbThemeLight)).setChecked(true);
        } else if (theme == UiPreferences.THEME_DARK) {
            ((RadioButton) findViewById(R.id.rbThemeDark)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rbThemeSystem)).setChecked(true);
        }
    }

    private void bindListeners() {
        rgFontSize.setOnCheckedChangeListener((group, checkedId) -> {
            float scale = UiPreferences.FONT_SCALE_NORMAL;
            if (checkedId == R.id.rbFontSmall) {
                scale = UiPreferences.FONT_SCALE_SMALL;
            } else if (checkedId == R.id.rbFontLarge) {
                scale = UiPreferences.FONT_SCALE_LARGE;
            }
            UiPreferences.saveFontScale(this, scale);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            recreate();
        });

        rgFontWeight.setOnCheckedChangeListener((group, checkedId) -> {
            UiPreferences.saveFontBold(this, checkedId == R.id.rbWeightBold);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            recreate();
        });

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = UiPreferences.THEME_SYSTEM;
            if (checkedId == R.id.rbThemeLight) {
                mode = UiPreferences.THEME_LIGHT;
            } else if (checkedId == R.id.rbThemeDark) {
                mode = UiPreferences.THEME_DARK;
            }
            UiPreferences.saveThemeMode(this, mode);
            AppCompatDelegate.setDefaultNightMode(mode);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            recreate();
        });
    }
}
