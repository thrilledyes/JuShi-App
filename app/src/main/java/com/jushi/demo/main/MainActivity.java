package com.jushi.demo.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jushi.demo.main.ui.imports.ImportTimetableActivity;
import com.jushi.demo.main.ui.imports.SelectSourceActivity;
import com.jushi.demo.main.ui.message.MessageFragment;
import com.jushi.demo.main.ui.settings.SettingsFragment;
import com.jushi.demo.main.ui.settings.UiPreferences;
import com.jushi.demo.main.ui.timetable.TimetableFragment;
import com.jushi.demo.main.ui.todo.TodoFragment;

public class MainActivity extends BaseActivity {
    private TextView tvPageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPageTitle = findViewById(R.id.tvPageTitle);
        ImageButton btnToolbar = findViewById(R.id.btnToolbar);
        ImageButton btnImport = findViewById(R.id.btnImport);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);

        btnToolbar.setOnClickListener(v ->
                Toast.makeText(this, "Toolbar 功能待接入", Toast.LENGTH_SHORT).show());
        btnImport.setOnClickListener(v -> showImportMenu(btnImport));
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_todo);
        }
        applyTitleStyle();
        applyGlobalTypography();
    }

    private void showImportMenu(ImageButton anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_import_actions, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this::onImportMenuItemClick);
        popupMenu.show();
    }

    private boolean onImportMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_select_source) {
            startActivity(new Intent(this, SelectSourceActivity.class));
            return true;
        }
        if (itemId == R.id.action_import_timetable) {
            startActivity(new Intent(this, ImportTimetableActivity.class));
            return true;
        }
        return false;
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_todo) {
            switchTo(new TodoFragment(), getString(R.string.nav_todo));
            return true;
        }
        if (itemId == R.id.nav_timetable) {
            switchTo(new TimetableFragment(), getString(R.string.nav_timetable));
            return true;
        }
        if (itemId == R.id.nav_message) {
            switchTo(new MessageFragment(), getString(R.string.nav_message));
            return true;
        }
        if (itemId == R.id.nav_settings) {
            switchTo(new SettingsFragment(), getString(R.string.nav_settings));
            return true;
        }
        return false;
    }

    private void switchTo(Fragment fragment, String title) {
        tvPageTitle.setText(title);
        applyTitleStyle();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        applyGlobalTypography();
    }

    private void applyTitleStyle() {
        float scale = UiPreferences.getFontScale(this);
        boolean bold = UiPreferences.isFontBold(this);
        tvPageTitle.setTextSize(18f * scale);
        tvPageTitle.setTypeface(tvPageTitle.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);
    }
}
