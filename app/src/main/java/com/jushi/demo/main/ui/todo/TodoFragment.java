package com.jushi.demo.main.ui.todo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jushi.demo.main.R;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.TodoMessage;
import com.jushi.demo.main.utils.PersonalTodoReminderScheduler;
import com.jushi.demo.main.utils.TodoCalendarExporter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Date;

public class TodoFragment extends Fragment {
    private static final String TAG = "TodoFragment";
    private static final String AUTO_TODO_TYPE = "ddl";
    private static final String PERSONAL_TODO_TYPE = "personal_todo";
    private static final int REQUEST_CALENDAR_PERMISSION = 4301;

    private LinearLayout todoList;
    private TextView emptyState;
    private final List<TodoMessage> currentTodos = new ArrayList<>();
    private final Set<Long> selectedTodoIds = new HashSet<>();
    private final List<Long> pendingCalendarImportIds = new ArrayList<>();
    private boolean selectionMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);
        todoList = view.findViewById(R.id.todoList);
        emptyState = view.findViewById(R.id.todoEmptyState);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodos();
    }

    private void loadTodos() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            DBManager db = null;
            List<TodoMessage> todos = new ArrayList<>();
            try {
                db = new DBManager(appContext);
                todos = db.getActionableTodoItems();
            } catch (Exception e) {
                Log.e(TAG, "加载待办失败", e);
            } finally {
                if (db != null) {
                    db.close();
                }
            }
            if (getActivity() == null) {
                return;
            }
            List<TodoMessage> result = todos;
            getActivity().runOnUiThread(() -> renderTodos(result));
        }).start();
    }

    private void renderTodos(List<TodoMessage> todos) {
        List<TodoMessage> snapshot = todos == currentTodos ? new ArrayList<>(todos) : todos;
        currentTodos.clear();
        if (snapshot != null) {
            currentTodos.addAll(snapshot);
        }
        syncSelectionWithCurrentTodos();

        todoList.removeAllViews();
        boolean empty = currentTodos.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            selectionMode = false;
            selectedTodoIds.clear();
            return;
        }

        if (selectionMode) {
            todoList.addView(createSelectionToolbar());
        }

        for (TodoMessage todo : currentTodos) {
            todoList.addView(createTodoRow(todo));
        }
    }

    private void syncSelectionWithCurrentTodos() {
        Set<Long> existingIds = new HashSet<>();
        for (TodoMessage todo : currentTodos) {
            existingIds.add(todo.id);
        }
        selectedTodoIds.retainAll(existingIds);
        if (selectionMode && selectedTodoIds.isEmpty()) {
            selectionMode = false;
        }
    }

    private View createSelectionToolbar() {
        LinearLayout toolbar = new LinearLayout(requireContext());
        toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(dp(12), dp(10), dp(12), dp(8));
        toolbar.setBackgroundColor(0xFFFFFFFF);

        TextView title = new TextView(requireContext());
        title.setText("已选择 " + selectedTodoIds.size() + " 项");
        title.setTextColor(0xFF15181D);
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        toolbar.addView(title);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(8), 0, 0);

        actions.addView(buildToolbarButton("一键全选", v -> selectAllTodos()), weightedButtonParams());
        actions.addView(buildToolbarButton("导入日历", v -> importTodosToCalendar(getSelectedTodos())), weightedButtonParams());
        actions.addView(buildToolbarButton("删除", v -> confirmDeleteTodos(new ArrayList<>(selectedTodoIds))), weightedButtonParams());
        actions.addView(buildToolbarButton("取消", v -> exitSelectionMode()), weightedButtonParams());
        toolbar.addView(actions);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(12), dp(6), dp(12), dp(8));
        toolbar.setLayoutParams(params);
        return toolbar;
    }

    private Button buildToolbarButton(String text, View.OnClickListener listener) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private View createTodoRow(TodoMessage todo) {
        Context context = requireContext();
        boolean completed = todo.completedAt > 0;

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(completed ? 0xFFE6E8EB : 0xFFFFFFFF);
        background.setCornerRadius(dp(8));
        card.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(12), dp(6), dp(12), dp(6));
        card.setLayoutParams(params);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        if (selectionMode) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setChecked(selectedTodoIds.contains(todo.id));
            checkBox.setOnClickListener(v -> toggleSelection(todo.id));
            top.addView(checkBox, new LinearLayout.LayoutParams(dp(40), dp(40)));
        }

        TextView title = new TextView(context);
        title.setText(buildDisplayTitle(todo));
        title.setTextColor(completed ? 0xFF7A7F87 : 0xFF15181D);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        applyStrike(title, completed);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView status = buildStatusChip(todo);
        top.addView(status);
        card.addView(top);

        TextView deadline = new TextView(context);
        deadline.setText("截止：" + nonEmpty(todo.deadline, "未设置"));
        deadline.setTextColor(resolveDeadlineColor(todo));
        deadline.setTextSize(14);
        deadline.setPadding(0, dp(8), 0, 0);
        applyStrike(deadline, completed);
        card.addView(deadline);

        TextView content = new TextView(context);
        content.setText(buildDisplayContent(todo));
        content.setTextColor(completed ? 0xFF7A7F87 : 0xFF555B63);
        content.setTextSize(14);
        content.setMaxLines(2);
        content.setEllipsize(TextUtils.TruncateAt.END);
        content.setPadding(0, dp(8), 0, 0);
        applyStrike(content, completed);
        card.addView(content);

        LinearLayout bottom = new LinearLayout(context);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(10), 0, 0);

        TextView source = new TextView(context);
        source.setText(buildSourceLabel(todo));
        source.setTextColor(0xFF8A9099);
        source.setTextSize(12);
        source.setSingleLine(true);
        source.setEllipsize(TextUtils.TruncateAt.END);
        bottom.addView(source, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!selectionMode) {
            TextView completeCircle = buildCompleteCircle(todo, completed);
            bottom.addView(completeCircle, new LinearLayout.LayoutParams(dp(34), dp(34)));
        }
        card.addView(bottom);

        card.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(todo.id);
            } else {
                openTodoDetail(todo.id);
            }
        });
        card.setOnLongClickListener(v -> {
            if (selectionMode) {
                toggleSelection(todo.id);
            } else {
                showTodoActions(todo);
            }
            return true;
        });
        return card;
    }

    private TextView buildCompleteCircle(TodoMessage todo, boolean completed) {
        TextView circle = new TextView(requireContext());
        circle.setGravity(Gravity.CENTER);
        circle.setText(completed ? "✓" : "");
        circle.setTextSize(18);
        circle.setTypeface(circle.getTypeface(), Typeface.BOLD);
        circle.setTextColor(0xFFFFFFFF);
        circle.setContentDescription(completed ? "取消完成" : "标记完成");

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(completed ? 0xFF1565C0 : 0xFFFFFFFF);
        bg.setStroke(dp(2), completed ? 0xFF1565C0 : 0xFF9CA3AF);
        circle.setBackground(bg);
        circle.setOnClickListener(v -> toggleTodoCompleted(todo));
        return circle;
    }

    private TextView buildStatusChip(TodoMessage todo) {
        TextView chip = new TextView(requireContext());
        chip.setTextSize(12);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(3), dp(8), dp(3));

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
        chip.setText(text);
        chip.setTextColor(textColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(6));
        chip.setBackground(bg);
        return chip;
    }

    private void toggleTodoCompleted(TodoMessage todo) {
        boolean nextCompleted = todo.completedAt <= 0;
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            DBManager db = new DBManager(appContext);
            try {
                db.setTodoCompleted(todo.id, nextCompleted);
            } finally {
                db.close();
            }
            if (nextCompleted) {
                PersonalTodoReminderScheduler.cancel(appContext, todo.id);
            } else {
                PersonalTodoReminderScheduler.scheduleAll(appContext);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::loadTodos);
            }
        }).start();
    }

    private void showTodoActions(TodoMessage todo) {
        String[] actions = {"删除", "加入手机日历日程", "多选"};
        new AlertDialog.Builder(requireContext())
                .setTitle(buildDisplayTitle(todo))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        List<Long> ids = new ArrayList<>();
                        ids.add(todo.id);
                        confirmDeleteTodos(ids);
                    } else if (which == 1) {
                        List<TodoMessage> todos = new ArrayList<>();
                        todos.add(todo);
                        importTodosToCalendar(todos);
                    } else {
                        enterSelectionMode(todo.id);
                    }
                })
                .show();
    }

    private void enterSelectionMode(long firstTodoId) {
        selectionMode = true;
        selectedTodoIds.clear();
        selectedTodoIds.add(firstTodoId);
        renderTodos(currentTodos);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectedTodoIds.clear();
        renderTodos(currentTodos);
    }

    private void toggleSelection(long todoId) {
        if (selectedTodoIds.contains(todoId)) {
            selectedTodoIds.remove(todoId);
        } else {
            selectedTodoIds.add(todoId);
        }
        renderTodos(currentTodos);
    }

    private void selectAllTodos() {
        selectedTodoIds.clear();
        for (TodoMessage todo : currentTodos) {
            selectedTodoIds.add(todo.id);
        }
        renderTodos(currentTodos);
    }

    private List<TodoMessage> getSelectedTodos() {
        List<TodoMessage> todos = new ArrayList<>();
        for (TodoMessage todo : currentTodos) {
            if (selectedTodoIds.contains(todo.id)) {
                todos.add(todo);
            }
        }
        return todos;
    }

    private void confirmDeleteTodos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择待办", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("删除待办")
                .setMessage("确认删除选中的 " + ids.size() + " 条待办？")
                .setPositiveButton("删除", (dialog, which) -> deleteTodos(ids))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteTodos(List<Long> ids) {
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            for (Long id : ids) {
                if (id != null) {
                    PersonalTodoReminderScheduler.cancel(appContext, id);
                }
            }
            DBManager db = new DBManager(appContext);
            try {
                db.deleteTodoMessages(ids);
            } finally {
                db.close();
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "已删除待办", Toast.LENGTH_SHORT).show();
                    selectionMode = false;
                    selectedTodoIds.clear();
                    loadTodos();
                });
            }
        }).start();
    }

    private void importTodosToCalendar(List<TodoMessage> todos) {
        if (todos == null || todos.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择待办", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasCalendarPermission()) {
            pendingCalendarImportIds.clear();
            for (TodoMessage todo : todos) {
                pendingCalendarImportIds.add(todo.id);
            }
            requestPermissions(
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    REQUEST_CALENDAR_PERMISSION
            );
            return;
        }
        exportTodosToCalendar(todos);
    }

    private void exportTodosToCalendar(List<TodoMessage> todos) {
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            TodoCalendarExporter.Result result = TodoCalendarExporter.exportTodos(appContext, todos);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), result.buildMessage(), Toast.LENGTH_LONG).show();
                    if (selectionMode) {
                        selectionMode = false;
                        selectedTodoIds.clear();
                        renderTodos(currentTodos);
                    }
                });
            }
        }).start();
    }

    private boolean hasCalendarPermission() {
        Context context = getContext();
        return context != null
                && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CALENDAR_PERMISSION) {
            return;
        }
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            granted = granted && result == PackageManager.PERMISSION_GRANTED;
        }
        if (!granted) {
            pendingCalendarImportIds.clear();
            Toast.makeText(requireContext(), "未获得日历权限，无法导入", Toast.LENGTH_SHORT).show();
            return;
        }
        List<TodoMessage> todos = getTodosByIds(pendingCalendarImportIds);
        pendingCalendarImportIds.clear();
        exportTodosToCalendar(todos);
    }

    private List<TodoMessage> getTodosByIds(List<Long> ids) {
        List<TodoMessage> todos = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return todos;
        }
        Set<Long> idSet = new HashSet<>(ids);
        for (TodoMessage todo : currentTodos) {
            if (idSet.contains(todo.id)) {
                todos.add(todo);
            }
        }
        return todos;
    }

    private void openTodoDetail(long todoId) {
        Intent intent = new Intent(requireContext(), TodoDetailActivity.class);
        intent.putExtra(TodoDetailActivity.EXTRA_TODO_ID, todoId);
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
            return 0xFF6B7280;
        }
        return date.getTime() < System.currentTimeMillis() ? 0xFFC62828 : 0xFF1565C0;
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

    private void applyStrike(TextView view, boolean completed) {
        if (completed) {
            view.setPaintFlags(view.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            view.setPaintFlags(view.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
