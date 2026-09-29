package com.jushi.demo.main.ui.imports;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.NotificationRule;

import java.util.ArrayList;
import java.util.List;

public class PreviewEditSourcesActivity extends BaseActivity {
    private final List<NotificationRule> rules = new ArrayList<>();
    private List<CourseBindingOptions.Option> courseOptions = new ArrayList<>();
    private NotificationRuleAdapter adapter;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_edit_sources);

        RecyclerView recyclerView = findViewById(R.id.rvNotificationRules);
        emptyText = findViewById(R.id.tvEmptyRules);

        adapter = new NotificationRuleAdapter(rules, this::deleteRule, this::editRuleCourse);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadRules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
    }

    private void loadRules() {
        DBManager db = new DBManager(this);
        try {
            rules.clear();
            rules.addAll(db.getEnabledNotificationRules());
            List<Course> courses = db.getAllCourses();
            courseOptions = CourseBindingOptions.fromCourses(courses);
        } finally {
            db.close();
        }

        adapter.notifyDataSetChanged();
        emptyText.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void deleteRule(NotificationRule rule) {
        if (rule == null) {
            return;
        }

        DBManager db = new DBManager(this);
        try {
            db.deleteNotificationRule(rule.id);
        } finally {
            db.close();
        }

        Toast.makeText(this, "监听规则已删除", Toast.LENGTH_SHORT).show();
        loadRules();
    }

    private void editRuleCourse(NotificationRule rule) {
        if (rule == null) {
            return;
        }

        CharSequence[] items = CourseBindingOptions.toDialogItems(courseOptions);
        int checked = CourseBindingOptions.findDialogIndex(courseOptions, rule.courseId, rule.courseName);
        new AlertDialog.Builder(this)
                .setTitle("更改绑定课程")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    CourseBindingOptions.Option option = CourseBindingOptions.optionAtDialogIndex(courseOptions, which);
                    int courseId = CourseBindingOptions.NO_COURSE_ID;
                    String courseName = "";
                    if (option != null) {
                        courseId = option.courseId;
                        courseName = option.courseName;
                    }

                    DBManager db = new DBManager(this);
                    try {
                        db.updateNotificationRuleCourse(rule.id, courseId, courseName);
                    } finally {
                        db.close();
                    }

                    Toast.makeText(this, "绑定课程已更新", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadRules();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
