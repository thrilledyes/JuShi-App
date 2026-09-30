package com.jushi.demo.main.ui.todo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
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

import java.util.List;

public class PersonalTodoEditActivity extends BaseActivity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildRoot("编辑待办"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodos();
    }

    private LinearLayout buildRoot(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(12), 0);
        bar.setBackgroundColor(0xFF1565C0);

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setColorFilter(0xFFFFFFFF);
        back.setBackgroundColor(0x00000000);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        bar.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scrollView = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(12), dp(12), dp(12));
        scrollView.addView(list);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button add = new Button(this);
        add.setText("添加待办");
        add.setOnClickListener(v -> startActivity(new Intent(this, PersonalTodoAddActivity.class)));
        root.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return root;
    }

    private void loadTodos() {
        new Thread(() -> {
            DBManager db = new DBManager(this);
            List<TodoMessage> todos;
            try {
                todos = db.getPersonalTodoItems();
            } finally {
                db.close();
            }
            runOnUiThread(() -> renderTodos(todos));
        }).start();
    }

    private void renderTodos(List<TodoMessage> todos) {
        list.removeAllViews();
        if (todos == null || todos.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无自己添加的待办");
            empty.setTextColor(0xFF7A7F87);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            list.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }
        for (TodoMessage todo : todos) {
            list.addView(createTodoRow(todo));
        }
    }

    private LinearLayout createTodoRow(TodoMessage todo) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.setBackgroundColor(0xFFFFFFFF);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(todo.title);
        title.setTextColor(0xFF1F2328);
        title.setTextSize(16);
        textBox.addView(title);

        TextView deadline = new TextView(this);
        deadline.setText("截止：" + todo.deadline);
        deadline.setTextColor(0xFF7A7F87);
        deadline.setTextSize(13);
        deadline.setPadding(0, dp(4), 0, 0);
        textBox.addView(deadline);

        row.addView(textBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button delete = new Button(this);
        delete.setText("删除");
        delete.setOnClickListener(v -> deleteTodo(todo.id));
        row.addView(delete, new LinearLayout.LayoutParams(dp(82), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private void deleteTodo(long id) {
        new Thread(() -> {
            PersonalTodoReminderScheduler.cancel(this, id);
            DBManager db = new DBManager(this);
            try {
                db.deleteTodoMessage(id);
            } finally {
                db.close();
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除待办", Toast.LENGTH_SHORT).show();
                loadTodos();
            });
        }).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
