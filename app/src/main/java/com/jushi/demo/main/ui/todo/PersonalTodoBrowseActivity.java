package com.jushi.demo.main.ui.todo;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.TodoMessage;

import java.util.List;

public class PersonalTodoBrowseActivity extends BaseActivity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildRoot());
        loadTodos();
    }

    private LinearLayout buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(8, 0, 12, 0);
        bar.setBackgroundColor(0xFF1565C0);

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setColorFilter(0xFFFFFFFF);
        back.setBackgroundColor(0x00000000);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView titleView = new TextView(this);
        titleView.setText("浏览待办");
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
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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
            empty.setText("暂无待办");
            empty.setTextColor(0xFF7A7F87);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            list.addView(empty);
            return;
        }
        for (TodoMessage todo : todos) {
            TextView item = new TextView(this);
            item.setText(todo.title + "\n截止：" + todo.deadline);
            item.setTextColor(0xFF1F2328);
            item.setTextSize(15);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setBackgroundColor(0xFFFFFFFF);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            list.addView(item, params);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
