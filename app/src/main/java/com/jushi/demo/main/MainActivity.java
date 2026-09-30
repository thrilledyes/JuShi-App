package com.jushi.demo.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jushi.demo.main.ui.imports.ImportTimetableActivity;
import com.jushi.demo.main.ui.imports.EasyHpcImportActivity;
import com.jushi.demo.main.ui.imports.SelectSourceActivity;
import com.jushi.demo.main.ui.message.MessageFragment;
import com.jushi.demo.main.ui.settings.SettingsFragment;
import com.jushi.demo.main.ui.settings.UiPreferences;
import com.jushi.demo.main.ui.timetable.TimetableFragment;
import com.jushi.demo.main.ui.todo.PersonalTodoAddActivity;
import com.jushi.demo.main.ui.todo.TodoFragment;

public class MainActivity extends BaseActivity {
    private TextView tvPageTitle;
    private int currentNavItemId = R.id.nav_todo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPageTitle = findViewById(R.id.tvPageTitle);
        ImageButton btnImport = findViewById(R.id.btnImport);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);

        btnImport.setOnClickListener(v -> showToolbarActionMenu(btnImport));
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_todo);
        }
        applyTitleStyle();
        applyGlobalTypography();
    }

    private void showToolbarActionMenu(ImageButton anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        if (currentNavItemId == R.id.nav_todo) {
            popupMenu.getMenuInflater().inflate(R.menu.menu_todo_actions, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this::onTodoMenuItemClick);
        } else {
            popupMenu.getMenuInflater().inflate(R.menu.menu_import_actions, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this::onImportMenuItemClick);
        }
        popupMenu.show();
    }

    private boolean onTodoMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_add_todo) {
            startActivity(new Intent(this, PersonalTodoAddActivity.class));
            return true;
        }
        return false;
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
        if (itemId == R.id.action_import_easyhpc) {
            startActivity(new Intent(this, EasyHpcImportActivity.class));
            return true;
        }
        return false;
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_todo) {
            currentNavItemId = itemId;
            switchTo(new TodoFragment(), getString(R.string.nav_todo));
            return true;
        }
        if (itemId == R.id.nav_timetable) {
            currentNavItemId = itemId;
            switchTo(new TimetableFragment(), getString(R.string.nav_timetable));
            return true;
        }
        if (itemId == R.id.nav_message) {
            currentNavItemId = itemId;
            switchTo(new MessageFragment(), getString(R.string.nav_message));
            return true;
        }
        if (itemId == R.id.nav_settings) {
            currentNavItemId = itemId;
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
