package com.radiodedios.gt.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class HistoryManager {
    private static final String PREF_NAME = "history_prefs";
    private static final String KEY_LAST_STATION_NAME = "last_station_name";
    private static final String KEY_LAST_STATION_URL = "last_station_url";
    private static final String KEY_LAST_STATION_IMG = "last_station_img";
    private static final String KEY_AUTO_PLAY = "auto_play";
    
    private final SharedPreferences prefs;

    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLastStation(String name, String url, String img) {
        prefs.edit()
            .putString(KEY_LAST_STATION_NAME, name)
            .putString(KEY_LAST_STATION_URL, url)
            .putString(KEY_LAST_STATION_IMG, img)
            .apply();
    }

    public String getLastStationName() {
        return prefs.getString(KEY_LAST_STATION_NAME, null);
    }

    public String getLastStationUrl() {
        return prefs.getString(KEY_LAST_STATION_URL, null);
    }
    
    public String getLastStationImg() {
        return prefs.getString(KEY_LAST_STATION_IMG, null);
    }

    public void setAutoPlay(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply();
    }

    public boolean isAutoPlayEnabled() {
        return prefs.getBoolean(KEY_AUTO_PLAY, false);
    }
    
    public boolean hasLastStation() {
        return getLastStationUrl() != null;
    }
}
