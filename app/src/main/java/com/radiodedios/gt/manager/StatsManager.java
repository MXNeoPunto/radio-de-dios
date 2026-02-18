package com.radiodedios.gt.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatsManager {
    private static StatsManager instance;
    private final SharedPreferences prefs;

    private static final String PREF_NAME = "stats_prefs";
    private static final String KEY_TOTAL_TIME = "total_time";
    private static final String KEY_MOST_PLAYED_STATION = "most_played_station";
    private static final String KEY_MOST_PLAYED_COUNT = "most_played_count";
    private static final String KEY_ACTIVE_DAY = "active_day";
    private static final String KEY_ACTIVE_DAY_TIME = "active_day_time";

    private StatsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized StatsManager getInstance(Context context) {
        if (instance == null) {
            instance = new StatsManager(context);
        }
        return instance;
    }

    public void addPlayTime(long ms) {
        long currentTotal = prefs.getLong(KEY_TOTAL_TIME, 0);
        prefs.edit().putLong(KEY_TOTAL_TIME, currentTotal + ms).apply();

        // Update daily stats
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long dailyTime = prefs.getLong("day_" + today, 0) + ms;
        prefs.edit().putLong("day_" + today, dailyTime).apply();

        // Check if today is most active
        long maxDayTime = prefs.getLong(KEY_ACTIVE_DAY_TIME, 0);
        if (dailyTime > maxDayTime) {
            prefs.edit()
                .putLong(KEY_ACTIVE_DAY_TIME, dailyTime)
                .putString(KEY_ACTIVE_DAY, today)
                .apply();
        }
    }

    public void incrementStationPlay(String stationName) {
        if (stationName == null) return;

        int count = prefs.getInt("count_" + stationName, 0) + 1;
        prefs.edit().putInt("count_" + stationName, count).apply();

        int maxCount = prefs.getInt(KEY_MOST_PLAYED_COUNT, 0);
        if (count > maxCount) {
            prefs.edit()
                .putInt(KEY_MOST_PLAYED_COUNT, count)
                .putString(KEY_MOST_PLAYED_STATION, stationName)
                .apply();
        }
    }

    public String getTotalPlayTimeFormatted() {
        long ms = prefs.getLong(KEY_TOTAL_TIME, 0);
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms / (1000 * 60)) % 60;
        return hours + "h " + minutes + "m";
    }

    public String getMostPlayedStation() {
        return prefs.getString(KEY_MOST_PLAYED_STATION, "N/A");
    }

    public String getMostActiveDay() {
        return prefs.getString(KEY_ACTIVE_DAY, "N/A");
    }

    public String getDataUsageFormatted() {
        long ms = prefs.getLong(KEY_TOTAL_TIME, 0);
        // Estimate 128 kbps = 16 KB/s
        long bytes = (ms / 1000) * 16 * 1024;

        if (bytes > 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
