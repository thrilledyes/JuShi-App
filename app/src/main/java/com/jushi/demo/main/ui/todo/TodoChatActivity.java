package com.jushi.demo.main.ui.todo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.CourseReminderRule;
import com.jushi.demo.main.database.model.TodoMessage;
import com.jushi.demo.main.utils.CourseReminderScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TodoChatActivity extends BaseActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONVERSATION_TITLE = "conversation_title";

    public static final String COURSE_ASSISTANT_ID = "course_assistant";
    public static final String PERSONAL_TODO_ID = "personal_todo";
    private static final String PREFS_NAME = "todo_chat_prefs";
    private static final String KEY_COURSE_PICKER_SHOWN = "course_picker_shown";

    private String conversationId;
    private String conversationTitle;
    private LinearLayout chatContent;
    private ScrollView chatScroll;
    private ImageButton addButton;
    private List<Course> futureCourses = new ArrayList<>();
    private List<CourseReminderRule> currentRules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_chat);

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        conversationTitle = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        if (TextUtils.isEmpty(conversationId)) {
            conversationId = "";
        }
        if (TextUtils.isEmpty(conversationTitle)) {
            conversationTitle = "待办";
        }

        TextView titleView = findViewById(R.id.tvChatTitle);
        titleView.setText(conversationTitle);
        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        addButton = findViewById(R.id.btnAddCourses);
        boolean hasActions = COURSE_ASSISTANT_ID.equals(conversationId) || PERSONAL_TODO_ID.equals(conversationId);
        addButton.setVisibility(hasActions ? View.VISIBLE : View.INVISIBLE);
        addButton.setOnClickListener(v -> {
            if (COURSE_ASSISTANT_ID.equals(conversationId)) {
                showCoursePicker(false);
            } else if (PERSONAL_TODO_ID.equals(conversationId)) {
                showPersonalTodoMenu(v);
            }
        });

        chatContent = findViewById(R.id.chatContent);
        chatScroll = findViewById(R.id.chatScroll);
        loadChat();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chatContent != null) {
            loadChat();
        }
    }

    private void loadChat() {
        new Thread(() -> {
            DBManager db = new DBManager(this);
            List<TodoMessage> messages;
            List<Course> courses = new ArrayList<>();
            List<CourseReminderRule> rules = new ArrayList<>();
            try {
                messages = db.getTodoMessages(conversationId);
                db.markTodoConversationRead(conversationId);
                if (COURSE_ASSISTANT_ID.equals(conversationId)) {
                    for (Course course : db.getAllCourses()) {
                        if (CourseReminderScheduler.hasFutureCourseOccurrence(this, course)) {
                            courses.add(course);
                        }
                    }
                    rules = db.getCourseReminderRules();
                }
            } finally {
                db.close();
            }

            List<Course> finalCourses = courses;
            List<CourseReminderRule> finalRules = rules;
            runOnUiThread(() -> {
                futureCourses = finalCourses;
                currentRules = finalRules;
                renderChat(messages);
                maybeShowFirstCoursePicker();
            });
        }).start();
    }

    private void renderChat(List<TodoMessage> messages) {
        chatContent.removeAllViews();

        if (messages == null || messages.isEmpty()) {
            addBotBubble("这里还没有待办消息。");
        } else {
            for (TodoMessage message : messages) {
                boolean fromUser = "user".equals(message.senderType);
                addMessageBubble(message, fromUser);
            }
        }

        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showPersonalTodoMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("编辑待办");
        menu.getMenu().add("浏览待办");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("编辑待办".equals(title)) {
                startActivity(new Intent(this, PersonalTodoEditActivity.class));
                return true;
            }
            if ("浏览待办".equals(title)) {
                startActivity(new Intent(this, PersonalTodoBrowseActivity.class));
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void maybeShowFirstCoursePicker() {
        if (!COURSE_ASSISTANT_ID.equals(conversationId)) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_COURSE_PICKER_SHOWN, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_COURSE_PICKER_SHOWN, true).apply();
        showCoursePicker(true);
    }

    private void showCoursePicker(boolean firstOpen) {
        LinkedHashMap<String, List<Course>> coursesByName = buildFutureCoursesByName();
        if (coursesByName.isEmpty()) {
            Toast.makeText(this, "当前时间之后没有可提醒课程", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = coursesByName.keySet().toArray(new String[0]);
        boolean[] checked = buildCheckedState(names, coursesByName);

        new AlertDialog.Builder(this)
                .setTitle(firstOpen ? "选择需要提醒的课程" : "添加提醒课程")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("保存", (dialog, which) -> saveCourseRules(names, checked, coursesByName))
                .setNegativeButton("取消", null)
                .show();
    }

    private LinkedHashMap<String, List<Course>> buildFutureCoursesByName() {
        LinkedHashMap<String, List<Course>> result = new LinkedHashMap<>();
        for (Course course : futureCourses) {
            String name = normalizeCourseName(course.getCourseName());
            if (name.isEmpty()) {
                continue;
            }
            List<Course> list = result.get(name);
            if (list == null) {
                list = new ArrayList<>();
                result.put(name, list);
            }
            list.add(course);
        }
        return result;
    }

    private boolean[] buildCheckedState(String[] names, Map<String, List<Course>> coursesByName) {
        Set<Integer> selectedCourseIds = new HashSet<>();
        for (CourseReminderRule rule : currentRules) {
            if (rule.enabled) {
                selectedCourseIds.add(rule.courseId);
            }
        }

        boolean[] checked = new boolean[names.length];
        for (int i = 0; i < names.length; i++) {
            List<Course> courses = coursesByName.get(names[i]);
            if (courses == null) {
                continue;
            }
            for (Course course : courses) {
                if (selectedCourseIds.contains(course.getId())) {
                    checked[i] = true;
                    break;
                }
            }
        }
        return checked;
    }

    private void saveCourseRules(String[] names, boolean[] checked, Map<String, List<Course>> coursesByName) {
        new Thread(() -> {
            List<CourseReminderRule> rules = new ArrayList<>();
            Set<String> selectedNames = new HashSet<>();
            for (int i = 0; i < names.length; i++) {
                if (!checked[i]) {
                    continue;
                }
                selectedNames.add(names[i]);
                List<Course> courses = coursesByName.get(names[i]);
                if (courses == null) {
                    continue;
                }
                for (Course course : courses) {
                    CourseReminderRule rule = new CourseReminderRule();
                    rule.courseId = course.getId();
                    rule.enabled = true;
                    rule.remindDayBefore = true;
                    rule.remindBeforeMinutes = 20;
                    rules.add(rule);
                }
            }

            DBManager db = new DBManager(this);
            try {
                db.replaceCourseReminderRules(rules);
                db.insertTodoMessage(
                        COURSE_ASSISTANT_ID,
                        "课程表助手",
                        "course_rule_saved",
                        "bot",
                        "提醒设置已保存",
                        buildSaveMessage(selectedNames),
                        "",
                        -1
                );
            } finally {
                db.close();
            }

            CourseReminderScheduler.scheduleAll(this);
            runOnUiThread(() -> {
                Toast.makeText(this, "课程提醒已保存", Toast.LENGTH_SHORT).show();
                loadChat();
            });
        }).start();
    }

    private String buildSaveMessage(Set<String> selectedNames) {
        if (selectedNames.isEmpty()) {
            return "已清空课程提醒。";
        }
        StringBuilder sb = new StringBuilder("已开启提醒课程：");
        boolean first = true;
        for (String name : selectedNames) {
            if (!first) {
                sb.append("、");
            }
            sb.append(name);
            first = false;
        }
        return sb.toString();
    }

    private void addMessageBubble(TodoMessage message, boolean fromUser) {
        StringBuilder text = new StringBuilder();
        if (!TextUtils.isEmpty(message.title)) {
            text.append(message.title).append("\n");
        }
        if (!TextUtils.isEmpty(message.content)) {
            text.append(message.content);
        }
        if (!TextUtils.isEmpty(message.deadline) && !text.toString().contains(message.deadline)) {
            text.append("\n截止：").append(message.deadline);
        }
        TextView bubble = addBubble(text.toString().trim(), fromUser, message.createdAt);
        bubble.setOnLongClickListener(v -> {
            showMessageActions(message);
            return true;
        });
    }

    private void showMessageActions(TodoMessage message) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"编辑", "删除"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditMessageDialog(message);
                    } else {
                        deleteMessage(message.id);
                    }
                })
                .show();
    }

    private void showEditMessageDialog(TodoMessage message) {
        EditText input = new EditText(this);
        input.setMinLines(3);
        input.setText(message.content);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("编辑消息内容")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> updateMessage(message.id, input.getText().toString().trim()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteMessage(long messageId) {
        runMessageAction(db -> db.deleteTodoMessage(messageId), "已删除消息");
    }

    private void updateMessage(long messageId, String content) {
        if (content.isEmpty()) {
            Toast.makeText(this, "消息内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        runMessageAction(db -> db.updateTodoMessageContent(messageId, content), "已更新消息");
    }

    private void runMessageAction(DbAction action, String toast) {
        new Thread(() -> {
            DBManager db = new DBManager(this);
            try {
                action.run(db);
            } finally {
                db.close();
            }
            runOnUiThread(() -> {
                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
                loadChat();
            });
        }).start();
    }

    private void addBotBubble(String content) {
        addBubble(content, false, System.currentTimeMillis());
    }

    private TextView addBubble(String content, boolean fromUser, long createdAt) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(fromUser ? Gravity.RIGHT : Gravity.LEFT);
        wrap.setPadding(0, dp(6), 0, dp(6));

        TextView time = new TextView(this);
        time.setText(formatTime(createdAt));
        time.setTextColor(0xFF9AA0A6);
        time.setTextSize(11);
        time.setGravity(Gravity.CENTER);
        wrap.addView(time, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView bubble = new TextView(this);
        bubble.setText(TextUtils.isEmpty(content) ? "空消息" : content);
        bubble.setTextSize(15);
        bubble.setLineSpacing(dp(2), 1f);
        bubble.setTextColor(fromUser ? 0xFFFFFFFF : 0xFF1F2328);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(fromUser ? 0xFF1976D2 : 0xFFFFFFFF);
        bg.setCornerRadius(dp(8));
        bubble.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dp(92), dp(300)),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(4);
        params.gravity = fromUser ? Gravity.RIGHT : Gravity.LEFT;
        wrap.addView(bubble, params);
        chatContent.addView(wrap);
        return bubble;
    }

    private String formatTime(long time) {
        if (time <= 0) {
            return "";
        }
        return new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(new Date(time));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String normalizeCourseName(String value) {
        return value == null ? "" : value.trim();
    }

    private interface DbAction {
        void run(DBManager db);
    }
}
