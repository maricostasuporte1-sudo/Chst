package com.casaemdia.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Store {
    private static final String PREFS = "casa_em_dia";
    private final SharedPreferences prefs;

    public Store(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains("people")) {
            List<String> initial = new ArrayList<>();
            initial.add("Michel");
            savePeople(initial);
        }
    }

    public List<String> getPeople() {
        List<String> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("people", "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(arr.getString(i));
        } catch (Exception ignored) {}
        return out;
    }

    public void savePeople(List<String> people) {
        JSONArray arr = new JSONArray();
        for (String p : people) arr.put(p);
        prefs.edit().putString("people", arr.toString()).apply();
    }

    public List<Task> getTasks() {
        List<Task> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("tasks", "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Task.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public void saveTasks(List<Task> tasks) {
        JSONArray arr = new JSONArray();
        for (Task t : tasks) {
            try { arr.put(t.toJson()); } catch (Exception ignored) {}
        }
        prefs.edit().putString("tasks", arr.toString()).apply();
    }

    public List<Task> getTodayTasks() {
        Calendar now = Calendar.getInstance();
        List<Task> out = new ArrayList<>();
        for (Task t : getTasks()) if (t.isFor(now)) out.add(t);
        return out;
    }

    private String completionKey() {
        return "done_" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(Calendar.getInstance().getTime());
    }

    public boolean isDone(String taskId) {
        Set<String> set = prefs.getStringSet(completionKey(), new HashSet<>());
        return set != null && set.contains(taskId);
    }

    public void setDone(String taskId, boolean done) {
        Set<String> current = prefs.getStringSet(completionKey(), new HashSet<>());
        Set<String> copy = current == null ? new HashSet<>() : new HashSet<>(current);
        if (done) copy.add(taskId); else copy.remove(taskId);
        prefs.edit().putStringSet(completionKey(), copy).apply();
    }

    public int getHour() { return prefs.getInt("notify_hour", 8); }
    public int getMinute() { return prefs.getInt("notify_minute", 0); }

    public void setNotifyTime(int hour, int minute) {
        prefs.edit().putInt("notify_hour", hour).putInt("notify_minute", minute).apply();
    }
}
