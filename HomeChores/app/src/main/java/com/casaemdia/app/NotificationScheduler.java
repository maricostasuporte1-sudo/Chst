package com.casaemdia.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.List;

public class NotificationScheduler {
    public static final String CHANNEL_ID = "tarefas_diarias";
    private static final int ALARM_REQUEST = 4455;

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Lembretes das tarefas", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Lembretes diários da escala da casa.");
            nm.createNotificationChannel(channel);
        }
    }

    public static void scheduleNext(Context context) {
        Store store = new Store(context);
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, store.getHour());
        next.set(Calendar.MINUTE, store.getMinute());
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);

        Intent intent = new Intent(context, ChoreReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    public static void showDaily(Context context) {
        ensureChannel(context);
        List<Task> tasks = new Store(context).getTodayTasks();
        if (tasks.isEmpty()) return;

        String text;
        if (tasks.size() == 1) {
            Task t = tasks.get(0);
            text = "Hoje o " + t.person + " vai " + lowerFirst(t.title) + ".";
        } else {
            StringBuilder sb = new StringBuilder();
            int max = Math.min(tasks.size(), 3);
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append(" • ");
                Task t = tasks.get(i);
                sb.append(t.person).append(": ").append(t.title);
            }
            if (tasks.size() > 3) sb.append(" • +").append(tasks.size() - 3);
            text = sb.toString();
        }
        showNow(context, "Tarefas de hoje", text);
    }

    public static void showNow(Context context, String title, String text) {
        ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1001, builder.build());
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
