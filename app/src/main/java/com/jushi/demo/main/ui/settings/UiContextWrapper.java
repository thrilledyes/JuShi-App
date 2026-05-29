package com.jushi.demo.main.ui.settings;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;

public final class UiContextWrapper extends ContextWrapper {
    private UiContextWrapper(Context base) {
        super(base);
    }

    public static Context wrap(Context base) {
        float fontScale = UiPreferences.getFontScale(base);
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        if (Math.abs(configuration.fontScale - fontScale) < 0.01f) {
            return base;
        }
        configuration.fontScale = fontScale;
        return base.createConfigurationContext(configuration);
    }
}
