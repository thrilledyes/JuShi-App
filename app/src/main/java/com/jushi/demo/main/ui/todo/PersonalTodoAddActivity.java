package com.jushi.demo.main.ui.todo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

public class PersonalTodoAddActivity extends BaseActivity {
    public static final String EXTRA_TODO_ID = "todo_id";

    private static final long NEW_TODO_ID = -1L;

    private long todoId = NEW_TODO_ID;
    private EditText titleInput;
    private EditText contentInput;
    private Button deadlineButton;
    private Calendar selectedDeadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todoId = getIntent().getLongExtra(EXTRA_TODO_ID, NEW_TODO_ID);
        setContentView(buildRoot());
        if (isEditing()) {
            loadTodo();
        }
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

        TextView titleView = new TextView(this);
        titleView.setText(isEditing() ? "编辑待办" : "添加待办");
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View spacer = new View(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(16), dp(16), dp(16));

        titleInput = new EditText(this);
        titleInput.setHint("待办名称（必填）");
        titleInput.setSingleLine(true);
        form.addView(titleInput);

        contentInput = new EditText(this);
        contentInput.setHint("待办内容（可选）");
        contentInput.setMinLines(4);
        form.addView(contentInput);

        deadlineButton = new Button(this);
        deadlineButton.setText("选择截止时间");
        deadlineButton.setAllCaps(false);
        deadlineButton.setOnClickListener(v -> showDeadlinePicker());
        form.addView(deadlineButton);

        Button save = new Button(this);
        save.setText(isEditing() ? "保存修改" : "保存待办");
        save.setOnClickListener(v -> saveTodo());
        form.addView(save);

        scrollView.addView(form);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
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
            runOnUiThread(() -> {
                if (todo == null) {
                    Toast.makeText(this, "待办不存在", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                titleInput.setText(todo.title);
                contentInput.setText(simplifyDetectedContent(todo.content));
                Date deadline = parseDeadline(todo.deadline);
                if (deadline != null) {
                    selectedDeadline = Calendar.getInstance();
                    selectedDeadline.setTime(deadline);
                    updateDeadlineButton();
                } else {
                    deadlineButton.setText("选择截止时间");
                }
            });
        }).start();
    }

    private void showDeadlinePicker() {
        Calendar initial = selectedDeadline == null ? Calendar.getInstance() : (Calendar) selectedDeadline.clone();
        if (selectedDeadline == null) {
            initial.add(Calendar.HOUR_OF_DAY, 1);
        }

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
                    showTimePicker(pickedDate);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private void showTimePicker(Calendar pickedDate) {
        TimePickerDialog timeDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    pickedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    pickedDate.set(Calendar.MINUTE, minute);
                    pickedDate.set(Calendar.SECOND, 0);
                    pickedDate.set(Calendar.MILLISECOND, 0);
                    selectedDeadline = pickedDate;
                    updateDeadlineButton();
                },
                pickedDate.get(Calendar.HOUR_OF_DAY),
                pickedDate.get(Calendar.MINUTE),
                true
        );
        timeDialog.show();
    }

    private void updateDeadlineButton() {
        deadlineButton.setText("截止时间：" + formatDeadline(selectedDeadline.getTime()));
    }

    private void saveTodo() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "待办名称不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedDeadline == null) {
            Toast.makeText(this, "请选择截止时间", Toast.LENGTH_LONG).show();
            return;
        }

        Date deadlineDate = selectedDeadline.getTime();
        if (!isEditing() && deadlineDate.getTime() <= System.currentTimeMillis()) {
            Toast.makeText(this, "截止时间必须晚于当前时间", Toast.LENGTH_LONG).show();
            return;
        }

        String deadline = formatDeadline(deadlineDate);
        new Thread(() -> {
            DBManager db = new DBManager(this);
            try {
                if (isEditing()) {
                    db.updateTodoMessage(todoId, title, content, deadline);
                } else {
                    db.insertTodoMessage(
                            TodoChatActivity.PERSONAL_TODO_ID,
                            "我的待办",
                            "personal_todo",
                            "user",
                            title,
                            content,
                            deadline,
                            -1
                    );
                }
            } finally {
                db.close();
            }
            PersonalTodoReminderScheduler.scheduleAll(this);
            runOnUiThread(() -> {
                Toast.makeText(this, isEditing() ? "待办已更新" : "待办已添加", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private boolean isEditing() {
        return todoId != NEW_TODO_ID;
    }

    private Date parseDeadline(String value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private String formatDeadline(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(value);
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
