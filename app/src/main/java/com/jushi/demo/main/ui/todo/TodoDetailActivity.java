package com.jushi.demo.main.ui.todo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.TodoMessage;
import com.jushi.demo.main.utils.PersonalTodoReminderScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TodoDetailActivity extends BaseActivity {
    public static final String EXTRA_TODO_ID = "todo_id";

    private static final String PERSONAL_TODO_TYPE = "personal_todo";
    private static final String AUTO_TODO_TYPE = "ddl";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 5301;

    private long todoId;
    private TodoMessage currentTodo;
    private TextView titleView;
    private TextView deadlineView;
    private TextView statusView;
    private TextView sourceView;
    private TextView contentView;
    private TextView createdAtView;
    private Button reminderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todoId = getIntent().getLongExtra(EXTRA_TODO_ID, -1L);
        if (todoId <= 0) {
            Toast.makeText(this, "待办不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(buildRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodo();
    }

    private LinearLayout buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(8), 0);
        bar.setBackgroundColor(0xFF1565C0);

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setColorFilter(0xFFFFFFFF);
        back.setBackgroundColor(0x00000000);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView pageTitle = new TextView(this);
        pageTitle.setText("待办详情");
        pageTitle.setTextColor(0xFFFFFFFF);
        pageTitle.setTextSize(18);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setTypeface(pageTitle.getTypeface(), Typeface.BOLD);
        bar.addView(pageTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton edit = new ImageButton(this);
        edit.setImageResource(android.R.drawable.ic_menu_edit);
        edit.setColorFilter(0xFFFFFFFF);
        edit.setBackgroundColor(0x00000000);
        edit.setContentDescription("编辑待办");
        edit.setOnClickListener(v -> openEditPage());
        bar.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(44)));

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(8));
        card.setBackground(bg);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        titleView = new TextView(this);
        titleView.setTextColor(0xFF15181D);
        titleView.setTextSize(20);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        statusView = new TextView(this);
        statusView.setTextSize(12);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(8), dp(3), dp(8), dp(3));
        titleRow.addView(statusView);
        card.addView(titleRow);

        deadlineView = buildDetailText(15, true);
        deadlineView.setPadding(0, dp(14), 0, 0);
        card.addView(deadlineView);

        sourceView = buildDetailText(14, false);
        card.addView(sourceView);

        createdAtView = buildDetailText(13, false);
        card.addView(createdAtView);

        TextView contentLabel = buildDetailText(14, true);
        contentLabel.setText("内容");
        contentLabel.setPadding(0, dp(18), 0, dp(6));
        card.addView(contentLabel);

        contentView = buildDetailText(15, false);
        contentView.setLineSpacing(dp(3), 1f);
        card.addView(contentView);

        content.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        reminderButton = new Button(this);
        reminderButton.setText("设置提醒");
        reminderButton.setAllCaps(false);
        reminderButton.setOnClickListener(v -> showReminderAction());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        editParams.topMargin = dp(16);
        content.addView(reminderButton, editParams);

        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private TextView buildDetailText(int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setTextSize(sp);
        view.setTextColor(0xFF333840);
        view.setPadding(0, dp(6), 0, 0);
        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }
        return view;
    }

    private void loadTodo() {
        new Thread(() -> {
            DBManager db = new DBManager(this);
            TodoMessage todo;
            try {
                todo = db.getTodoMessageById(todoId);
            } finally {
                db.close();
            }
            runOnUiThread(() -> renderTodo(todo));
        }).start();
    }

    private void renderTodo(TodoMessage todo) {
        if (todo == null) {
            Toast.makeText(this, "待办不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentTodo = todo;
        titleView.setText(buildDisplayTitle(todo));
        deadlineView.setText("截止时间：" + nonEmpty(todo.deadline, "未设置"));
        deadlineView.setTextColor(resolveDeadlineColor(todo));
        sourceView.setText(buildSourceLabel(todo));
        createdAtView.setText("创建时间：" + formatCreatedAt(todo.createdAt));
        contentView.setText(buildDisplayContent(todo));
        updateStatus(todo);
        updateReminderButton(todo);
    }

    private void updateStatus(TodoMessage todo) {
        String text = "待处理";
        int textColor = 0xFF1565C0;
        int bgColor = 0xFFE3F2FD;
        if (todo.completedAt > 0) {
            text = "已完成";
            textColor = 0xFF6B7280;
            bgColor = 0xFFD1D5DB;
        } else {
            Date date = parseDeadline(todo.deadline);
            if (date != null) {
                long diff = date.getTime() - System.currentTimeMillis();
                if (diff < 0) {
                    text = "已过期";
                    textColor = 0xFFC62828;
                    bgColor = 0xFFFFEBEE;
                } else if (diff <= 24L * 60L * 60L * 1000L) {
                    text = "今天";
                    textColor = 0xFFEF6C00;
                    bgColor = 0xFFFFF3E0;
                }
            }
        }
        statusView.setText(text);
        statusView.setTextColor(textColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(6));
        statusView.setBackground(bg);
    }

    private void updateReminderButton(TodoMessage todo) {
        if (todo.completedAt > 0) {
            reminderButton.setText("已完成，无需提醒");
            reminderButton.setEnabled(false);
            return;
        }
        reminderButton.setEnabled(true);
        if (todo.reminderAt > 0) {
            reminderButton.setText("提醒时间：" + formatMillis(todo.reminderAt));
        } else {
            reminderButton.setText("设置提醒");
        }
    }

    private void showReminderAction() {
        if (currentTodo == null) {
            return;
        }
        if (currentTodo.reminderAt <= 0) {
            showReminderDatePicker();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("待办提醒")
                .setItems(new String[]{"重新选择提醒时间", "取消提醒"}, (dialog, which) -> {
                    if (which == 0) {
                        showReminderDatePicker();
                    } else {
                        cancelReminder();
                    }
                })
                .show();
    }

    private void showReminderDatePicker() {
        Calendar initial = buildInitialReminderTime();
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar pickedDate = Calendar.getInstance();
                    pickedDate.set(
                            year,
                            month,
                            dayOfMonth,
                            initial.get(Calendar.HOUR_OF_DAY),
                            initial.get(Calendar.MINUTE),
                            0
                    );
                    pickedDate.set(Calendar.MILLISECOND, 0);
                    showReminderTimePicker(pickedDate);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private Calendar buildInitialReminderTime() {
        Calendar initial = Calendar.getInstance();
        if (currentTodo != null && currentTodo.reminderAt > 0) {
            initial.setTimeInMillis(currentTodo.reminderAt);
            return initial;
        }
        Date deadline = currentTodo == null ? null : parseDeadline(currentTodo.deadline);
        if (deadline != null && deadline.getTime() > System.currentTimeMillis() + 60L * 60L * 1000L) {
            initial.setTimeInMillis(deadline.getTime() - 60L * 60L * 1000L);
        } else {
            initial.add(Calendar.HOUR_OF_DAY, 1);
        }
        initial.set(Calendar.SECOND, 0);
        initial.set(Calendar.MILLISECOND, 0);
        return initial;
    }

    private void showReminderTimePicker(Calendar pickedDate) {
        TimePickerDialog timeDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    pickedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    pickedDate.set(Calendar.MINUTE, minute);
                    pickedDate.set(Calendar.SECOND, 0);
                    pickedDate.set(Calendar.MILLISECOND, 0);
                    saveReminder(pickedDate.getTimeInMillis());
                },
                pickedDate.get(Calendar.HOUR_OF_DAY),
                pickedDate.get(Calendar.MINUTE),
                true
        );
        timeDialog.show();
    }

    private void saveReminder(long reminderAt) {
        if (reminderAt <= System.currentTimeMillis() + 30_000L) {
            Toast.makeText(this, "提醒时间必须晚于当前时间", Toast.LENGTH_SHORT).show();
            return;
        }
        ensureNotificationPermission();
        new Thread(() -> {
            DBManager db = new DBManager(this);
            TodoMessage todo;
            try {
                db.updateTodoReminder(todoId, reminderAt);
                todo = db.getTodoMessageById(todoId);
            } finally {
                db.close();
            }
            if (todo != null) {
                PersonalTodoReminderScheduler.scheduleTodo(this, todo);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "提醒已设置", Toast.LENGTH_SHORT).show();
                loadTodo();
            });
        }).start();
    }

    private void cancelReminder() {
        new Thread(() -> {
            PersonalTodoReminderScheduler.cancel(this, todoId);
            DBManager db = new DBManager(this);
            try {
                db.updateTodoReminder(todoId, 0);
            } finally {
                db.close();
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "提醒已取消", Toast.LENGTH_SHORT).show();
                loadTodo();
            });
        }).start();
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
    }

    private void openEditPage() {
        Intent intent = new Intent(this, PersonalTodoAddActivity.class);
        intent.putExtra(PersonalTodoAddActivity.EXTRA_TODO_ID, todoId);
        startActivity(intent);
    }

    private String buildSourceLabel(TodoMessage todo) {
        if (PERSONAL_TODO_TYPE.equals(todo.messageType)) {
            return "来源：手动添加";
        }
        String source = nonEmpty(todo.conversationTitle, todo.conversationId);
        return "来源：" + nonEmpty(source, "自动识别");
    }

    private String buildDisplayContent(TodoMessage todo) {
        String content = simplifyDetectedContent(todo.content);
        if (!TextUtils.isEmpty(content)) {
            return content;
        }
        return nonEmpty(todo.title, "暂无说明");
    }

    private String buildDisplayTitle(TodoMessage todo) {
        if (todo == null) {
            return "未命名待办";
        }
        if (shouldUseGeneratedAutoTitle(todo)) {
            String source = nonEmpty(todo.conversationTitle, todo.conversationId);
            return "来自" + nonEmpty(source, "群聊") + "的待办";
        }
        return nonEmpty(todo.title, "未命名待办");
    }

    private boolean shouldUseGeneratedAutoTitle(TodoMessage todo) {
        if (!AUTO_TODO_TYPE.equals(todo.messageType)) {
            return false;
        }
        String title = todo.title == null ? "" : todo.title.trim();
        if (title.isEmpty()) {
            return true;
        }
        String content = simplifyDetectedContent(todo.content);
        return !content.isEmpty() && title.equals(content);
    }

    private String simplifyDetectedContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        String value = content.trim();
        int taskIndex = value.indexOf("任务：");
        int deadlineIndex = value.indexOf("\n截止：");
        if (taskIndex >= 0 && deadlineIndex > taskIndex) {
            return value.substring(taskIndex + "任务：".length(), deadlineIndex).trim();
        }
        return value;
    }

    private int resolveDeadlineColor(TodoMessage todo) {
        if (todo.completedAt > 0) {
            return 0xFF7A7F87;
        }
        Date date = parseDeadline(todo.deadline);
        if (date == null) {
            return 0xFF333840;
        }
        return date.getTime() < System.currentTimeMillis() ? 0xFFC62828 : 0xFF1565C0;
    }

    private String formatCreatedAt(long createdAt) {
        if (createdAt <= 0) {
            return "未知";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(createdAt));
    }

    private String formatMillis(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private Date parseDeadline(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
