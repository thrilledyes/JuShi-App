package com.jushi.demo.main.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.ui.todo.TodoChatActivity;
import com.jushi.demo.main.ui.todo.TodoDetailActivity;

public class PersonalTodoReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "todo_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PersonalTodoReminderScheduler.scheduleAll(context);
            return;
        }

        long todoId = intent.getLongExtra(PersonalTodoReminderScheduler.EXTRA_TODO_ID, -1L);
        String title = intent.getStringExtra(PersonalTodoReminderScheduler.EXTRA_TODO_TITLE);
        String deadline = intent.getStringExtra(PersonalTodoReminderScheduler.EXTRA_TODO_DEADLINE);
        if (title == null || title.trim().isEmpty()) {
            title = "待办提醒";
        }
        showNotification(context, todoId, title, deadline);

        DBManager db = new DBManager(context.getApplicationContext());
        try {
            db.insertTodoMessage(
                    TodoChatActivity.PERSONAL_TODO_ID,
                    "我的待办",
                    "personal_reminder",
                    "bot",
                    "待办即将截止",
                    title + "\n截止：" + (deadline == null ? "未设置" : deadline),
                    deadline == null ? "" : deadline,
                    -1
            );
        } finally {
            db.close();
        }
    }

    private void showNotification(Context context, long todoId, String title, String deadline) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "待办提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        Intent detailIntent = new Intent(context, TodoDetailActivity.class);
        detailIntent.putExtra(TodoDetailActivity.EXTRA_TODO_ID, todoId);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (todoId % Integer.MAX_VALUE),
                detailIntent,
                pendingFlags
        );

        String content = deadline == null || deadline.trim().isEmpty()
                ? "待办提醒时间到了"
                : "截止时间：" + deadline;
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .build();

        try {
            manager.notify((int) (todoId % Integer.MAX_VALUE), notification);
        } catch (SecurityException ignored) {
        }
    }
}
