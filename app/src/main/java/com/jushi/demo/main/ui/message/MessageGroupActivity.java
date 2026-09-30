package com.jushi.demo.main.ui.message;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.RawMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageGroupActivity extends BaseActivity {
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_APP_NAME = "app_name";
    public static final String EXTRA_GROUP_NAME = "group_name";

    private String sessionId;
    private String appName;
    private String groupName;
    private LinearLayout messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "消息来源不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(buildRoot());
        loadMessages();
    }

    private LinearLayout buildRoot() {
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

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(nonEmpty(groupName, "群聊消息"));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(17);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titleBox.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(nonEmpty(appName, "未知应用"));
        subtitle.setTextColor(0xFFE3F2FD);
        subtitle.setTextSize(12);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        titleBox.addView(subtitle);

        bar.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scrollView = new ScrollView(this);
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(messageList);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void loadMessages() {
        new Thread(() -> {
            DBManager db = new DBManager(this);
            List<RawMessage> messages;
            try {
                messages = db.getRecentMessages(sessionId, 500);
            } finally {
                db.close();
            }
            runOnUiThread(() -> renderMessages(messages));
        }).start();
    }

    private void renderMessages(List<RawMessage> messages) {
        messageList.removeAllViews();
        if (messages == null || messages.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无已捕获的原始消息");
            empty.setTextColor(0xFF6B7280);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(80), 0, 0);
            messageList.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }

        for (RawMessage message : messages) {
            messageList.addView(buildMessageCard(message));
        }
    }

    private LinearLayout buildMessageCard(RawMessage message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(buildBg());

        TextView time = textView("捕获时间：" + formatTime(message.timestamp), 12, false, 0xFF6B7280);
        card.addView(time);

        TextView content = textView(nonEmpty(message.content, "无正文"), 15, false, 0xFF1F2937);
        content.setPadding(0, dp(8), 0, 0);
        content.setLineSpacing(dp(3), 1f);
        card.addView(content);

        String sender = nonEmpty(message.sender, "");
        if (!sender.isEmpty()) {
            TextView source = textView("通知标题：" + sender, 12, false, 0xFF8A9099);
            source.setPadding(0, dp(8), 0, 0);
            card.addView(source);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        return card;
    }

    private TextView textView(String text, int sp, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable buildBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(8));
        return bg;
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "未知";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
