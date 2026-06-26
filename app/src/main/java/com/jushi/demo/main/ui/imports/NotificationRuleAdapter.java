package com.jushi.demo.main.ui.imports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jushi.demo.main.R;
import com.jushi.demo.main.database.model.NotificationRule;

import java.util.List;

public class NotificationRuleAdapter extends RecyclerView.Adapter<NotificationRuleAdapter.RuleViewHolder> {
    public interface OnDeleteClickListener {
        void onDelete(NotificationRule rule);
    }

    public interface OnCourseClickListener {
        void onCourseClick(NotificationRule rule);
    }

    private final List<NotificationRule> rules;
    private final OnDeleteClickListener deleteClickListener;
    private final OnCourseClickListener courseClickListener;

    public NotificationRuleAdapter(
            List<NotificationRule> rules,
            OnDeleteClickListener deleteClickListener,
            OnCourseClickListener courseClickListener
    ) {
        this.rules = rules;
        this.deleteClickListener = deleteClickListener;
        this.courseClickListener = courseClickListener;
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        NotificationRule rule = rules.get(position);
        String groupName = isEmpty(rule.groupName) ? "全部消息" : rule.groupName;
        holder.titleText.setText(safe(rule.sourceName) + " / " + groupName);
        holder.subtitleText.setText(safe(rule.packageName)
                + "\n绑定课程: " + CourseBindingOptions.displayCourseName(rule.courseName));
        holder.courseButton.setOnClickListener(v -> {
            if (courseClickListener != null) {
                courseClickListener.onCourseClick(rule);
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDelete(rule);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class RuleViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView subtitleText;
        final Button courseButton;
        final Button deleteButton;

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tvRuleTitle);
            subtitleText = itemView.findViewById(R.id.tvRuleSubtitle);
            courseButton = itemView.findViewById(R.id.btnEditRuleCourse);
            deleteButton = itemView.findViewById(R.id.btnDeleteRule);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
