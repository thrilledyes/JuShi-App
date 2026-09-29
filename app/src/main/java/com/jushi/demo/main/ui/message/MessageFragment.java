package com.jushi.demo.main.ui.message;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jushi.demo.main.R;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.RawMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageFragment extends Fragment {
    private LinearLayout messageList;
    private TextView emptyState;
    private final List<AppNode> appNodes = new ArrayList<>();
    private final java.util.Set<String> expandedApps = new java.util.HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messageList = view.findViewById(R.id.messageList);
        emptyState = view.findViewById(R.id.messageEmptyState);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCapturedMessages();
    }

    private void loadCapturedMessages() {
        new Thread(() -> {
            DBManager db = new DBManager(requireContext());
            List<RawMessage> messages;
            try {
                messages = db.getCapturedRawMessages(1000);
            } finally {
                db.close();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> renderMessages(messages));
            }
        }).start();
    }

    private void renderMessages(List<RawMessage> messages) {
        appNodes.clear();
        appNodes.addAll(groupMessages(messages));

        messageList.removeAllViews();
        if (appNodes.isEmpty()) {
            emptyState.setText("还没有成功捕获的原始消息");
            emptyState.setVisibility(View.VISIBLE);
            messageList.addView(emptyState);
            return;
        }

        emptyState.setVisibility(View.GONE);
        for (AppNode appNode : appNodes) {
            messageList.addView(buildAppRow(appNode));
            if (expandedApps.contains(appNode.packageName)) {
                for (GroupNode groupNode : appNode.groups) {
                    messageList.addView(buildGroupRow(appNode, groupNode));
                }
            }
        }
    }

    private List<AppNode> groupMessages(List<RawMessage> messages) {
        Map<String, AppNode> apps = new LinkedHashMap<>();
        if (messages == null) {
            return new ArrayList<>();
        }

        for (RawMessage message : messages) {
            String packageName = extractPackageName(message.sessionId);
            String groupName = extractGroupName(message.sessionId, message.sender);
            AppNode appNode = apps.get(packageName);
            if (appNode == null) {
                appNode = new AppNode(packageName, appLabel(packageName, message));
                apps.put(packageName, appNode);
            }

            GroupNode groupNode = appNode.groupMap.get(groupName);
            if (groupNode == null) {
                groupNode = new GroupNode(message.sessionId, groupName);
                appNode.groupMap.put(groupName, groupNode);
                appNode.groups.add(groupNode);
            }

            appNode.messageCount++;
            groupNode.messageCount++;
            if (message.timestamp > appNode.latestTime) {
                appNode.latestTime = message.timestamp;
            }
            if (message.timestamp > groupNode.latestTime) {
                groupNode.latestTime = message.timestamp;
            }
        }
        return new ArrayList<>(apps.values());
    }

    private View buildAppRow(AppNode appNode) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(buildBg(0xFFFFFFFF, 8));

        LinearLayout top = new LinearLayout(requireContext());
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView name = textView(appNode.appName, 17, true, 0xFF15181D);
        top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView action = textView(expandedApps.contains(appNode.packageName) ? "收起" : "展开", 13, false, 0xFF1565C0);
        top.addView(action);
        row.addView(top);

        String meta = appNode.groups.size() + " 个群聊 · " + appNode.messageCount
                + " 条消息 · 最近 " + formatTime(appNode.latestTime);
        TextView info = textView(meta, 13, false, 0xFF6B7280);
        info.setPadding(0, dp(6), 0, 0);
        row.addView(info);

        row.setOnClickListener(v -> {
            if (expandedApps.contains(appNode.packageName)) {
                expandedApps.remove(appNode.packageName);
            } else {
                expandedApps.add(appNode.packageName);
            }
            renderMessagesFromCurrentNodes();
        });

        row.setLayoutParams(rowParams(0, dp(10)));
        return row;
    }

    private View buildGroupRow(AppNode appNode, GroupNode groupNode) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(18), dp(12), dp(14), dp(12));
        row.setBackground(buildBg(0xFFF8FAFC, 8));

        TextView name = textView(groupNode.groupName, 15, true, 0xFF1F2937);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(name);

        String meta = groupNode.messageCount + " 条已捕获消息 · 最近 " + formatTime(groupNode.latestTime);
        TextView info = textView(meta, 12, false, 0xFF6B7280);
        info.setPadding(0, dp(4), 0, 0);
        row.addView(info);

        row.setOnClickListener(v -> openGroupMessages(appNode, groupNode));
        row.setLayoutParams(rowParams(dp(20), dp(8)));
        return row;
    }

    private void renderMessagesFromCurrentNodes() {
        messageList.removeAllViews();
        for (AppNode appNode : appNodes) {
            messageList.addView(buildAppRow(appNode));
            if (expandedApps.contains(appNode.packageName)) {
                for (GroupNode groupNode : appNode.groups) {
                    messageList.addView(buildGroupRow(appNode, groupNode));
                }
            }
        }
    }

    private void openGroupMessages(AppNode appNode, GroupNode groupNode) {
        Intent intent = new Intent(requireContext(), MessageGroupActivity.class);
        intent.putExtra(MessageGroupActivity.EXTRA_SESSION_ID, groupNode.sessionId);
        intent.putExtra(MessageGroupActivity.EXTRA_APP_NAME, appNode.appName);
        intent.putExtra(MessageGroupActivity.EXTRA_GROUP_NAME, groupNode.groupName);
        startActivity(intent);
    }

    private TextView textView(String text, int sp, boolean bold, int color) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }
        return view;
    }

    private GradientDrawable buildBg(int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        return bg;
    }

    private LinearLayout.LayoutParams rowParams(int leftMargin, int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(leftMargin, 0, 0, bottomMargin);
        return params;
    }

    private String extractPackageName(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return "unknown";
        }
        int index = sessionId.indexOf('_');
        return index < 0 ? sessionId : sessionId.substring(0, index);
    }

    private String extractGroupName(String sessionId, String sender) {
        if (sessionId != null) {
            int index = sessionId.indexOf('_');
            if (index >= 0 && index + 1 < sessionId.length()) {
                String groupName = sessionId.substring(index + 1);
                if (!groupName.trim().isEmpty() && !"unknown".equals(groupName)) {
                    return groupName;
                }
            }
        }
        return sender == null || sender.trim().isEmpty() ? "未知群聊" : sender.trim();
    }

    private String appLabel(String packageName, RawMessage message) {
        if ("com.tencent.mm".equals(packageName)) {
            return "微信";
        }
        if ("com.tencent.mobileqq".equals(packageName)) {
            return "QQ";
        }
        if ("com.tencent.wework".equals(packageName)) {
            return "企业微信";
        }
        if ("com.chaoxing.mobile".equals(packageName)) {
            return "学习通";
        }
        if ("com.huawei.easyhpc".equals(packageName)) {
            return "超算习堂";
        }
        if ("com.yuketang.app".equals(packageName)) {
            return "雨课堂";
        }
        if (message != null && message.source != null) {
            return message.source.name();
        }
        return packageName == null || packageName.isEmpty() ? "未知应用" : packageName;
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "未知";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppNode {
        final String packageName;
        final String appName;
        final Map<String, GroupNode> groupMap = new LinkedHashMap<>();
        final List<GroupNode> groups = new ArrayList<>();
        int messageCount;
        long latestTime;

        AppNode(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
    }

    private static class GroupNode {
        final String sessionId;
        final String groupName;
        int messageCount;
        long latestTime;

        GroupNode(String sessionId, String groupName) {
            this.sessionId = sessionId;
            this.groupName = groupName;
        }
    }
}
