package com.jushi.demo.main.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.database.model.TodoMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PersonalTodoReminderScheduler {
    public static final String EXTRA_TODO_ID = "todo_id";
    public static final String EXTRA_TODO_TITLE = "todo_title";
    public static final String EXTRA_TODO_DEADLINE = "todo_deadline";
    private static final String PERSONAL_TODO_TYPE = "personal_todo";

    private PersonalTodoReminderScheduler() {
    }

    public static void scheduleAll(Context context) {
        DBManager db = new DBManager(context.getApplicationContext());
        List<TodoMessage> todos;
        try {
            todos = db.getActionableTodoItems();
        } finally {
            db.close();
        }
        for (TodoMessage todo : todos) {
            scheduleTodo(context, todo);
        }
    }

    public static void scheduleTodo(Context context, TodoMessage todo) {
        if (context == null || todo == null) {
            return;
        }
        cancel(context, todo.id);
        scheduleOne(context, todo);
    }

    public static void cancel(Context context, long todoId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(buildPendingIntent(context, todoId, "", ""));
        }
    }

    private static void scheduleOne(Context context, TodoMessage todo) {
        if (todo.completedAt > 0) {
            return;
        }
        long triggerAt = resolveTriggerAt(todo);
        if (triggerAt <= System.currentTimeMillis() + 30_000L) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildPendingIntent(context, todo.id, todo.title, todo.deadline);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static long resolveTriggerAt(TodoMessage todo) {
        if (todo.reminderAt > 0) {
            return todo.reminderAt;
        }
        if (!PERSONAL_TODO_TYPE.equals(todo.messageType)) {
            return 0L;
        }
        Date deadline = parseDeadline(todo.deadline);
        if (deadline == null) {
            return 0L;
        }
        return deadline.getTime() - 24L * 60L * 60L * 1000L;
    }

    private static PendingIntent buildPendingIntent(Context context, long todoId, String title, String deadline) {
        Intent intent = new Intent(context, PersonalTodoReminderReceiver.class);
        intent.putExtra(EXTRA_TODO_ID, todoId);
        intent.putExtra(EXTRA_TODO_TITLE, title);
        intent.putExtra(EXTRA_TODO_DEADLINE, deadline);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) (todoId % Integer.MAX_VALUE), intent, flags);
    }

    private static Date parseDeadline(String value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }
}
