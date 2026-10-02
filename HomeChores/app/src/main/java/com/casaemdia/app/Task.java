package com.casaemdia.app;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;

public class Task {
    public String id;
    public String title;
    public String person;
    public int daysMask;

    public Task(String id, String title, String person, int daysMask) {
        this.id = id;
        this.title = title;
        this.person = person;
        this.daysMask = daysMask;
    }

    public boolean isFor(Calendar calendar) {
        int androidDay = calendar.get(Calendar.DAY_OF_WEEK);
        int mondayIndex = (androidDay + 5) % 7;
        return (daysMask & (1 << mondayIndex)) != 0;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("person", person);
        o.put("daysMask", daysMask);
        return o;
    }

    public static Task fromJson(JSONObject o) throws JSONException {
        return new Task(o.getString("id"), o.getString("title"), o.getString("person"), o.getInt("daysMask"));
    }
}
