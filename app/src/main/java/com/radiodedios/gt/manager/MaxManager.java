package com.radiodedios.gt.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class MaxManager {
    private static final String PREF_NAME = "max_prefs";
    private static final String KEY_MAX_EXPIRATION = "max_expiration_time";
    private static final String KEY_DAILY_ACTIVATIONS = "daily_activations";
    private static final String KEY_LAST_ACTIVATION_DATE = "last_activation_date";
    private static final String KEY_NEXT_AD_AVAILABLE_TIME = "next_ad_available_time";

    // 30 minutes in milliseconds
    private static final long MAX_DURATION_MS = 30 * 60 * 1000;
    private static final int MAX_DAILY_LIMIT = 5;

    private final SharedPreferences prefs;

    public MaxManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isMaxActive() {
        long expiration = prefs.getLong(KEY_MAX_EXPIRATION, 0);
        return System.currentTimeMillis() < expiration;
    }

    public void activateMax() {
        addTime(MAX_DURATION_MS);
    }

    public void addTime(long durationMs) {
        long now = System.currentTimeMillis();
        long currentExpiration = prefs.getLong(KEY_MAX_EXPIRATION, 0);
        long newExpiration = (currentExpiration > now ? currentExpiration : now) + durationMs;
        
        // Reset daily count if it's a new day
        long lastDate = prefs.getLong(KEY_LAST_ACTIVATION_DATE, 0);
        if (!isSameDay(now, lastDate)) {
            prefs.edit().putInt(KEY_DAILY_ACTIVATIONS, 0).apply();
        }

        int currentActivations = prefs.getInt(KEY_DAILY_ACTIVATIONS, 0);
        // We increment regardless of limit here assuming the check was done before calling addTime
        // But to be safe we can check, or rely on canShowAd caller.
        // We'll update the activation count.
        
        prefs.edit()
            .putLong(KEY_MAX_EXPIRATION, newExpiration)
            .putInt(KEY_DAILY_ACTIVATIONS, currentActivations + 1)
            .putLong(KEY_LAST_ACTIVATION_DATE, now)
            .apply();
    }

    public boolean canActivateMax() {
        return canShowAd();
    }

    public boolean canShowAd() {
        long now = System.currentTimeMillis();
        
        // Check Block Time
        long nextAdTime = prefs.getLong(KEY_NEXT_AD_AVAILABLE_TIME, 0);
        if (now < nextAdTime) return false;

        long lastDate = prefs.getLong(KEY_LAST_ACTIVATION_DATE, 0);
        
        // If it's a different day, we can activate (count resets)
        if (!isSameDay(now, lastDate)) return true;

        // If same day, check limit
        return prefs.getInt(KEY_DAILY_ACTIVATIONS, 0) < MAX_DAILY_LIMIT;
    }

    public void blockAdsFor(long durationMs) {
        long now = System.currentTimeMillis();
        prefs.edit().putLong(KEY_NEXT_AD_AVAILABLE_TIME, now + durationMs).apply();
    }

    public long getRemainingAdBlockTime() {
        long nextAdTime = prefs.getLong(KEY_NEXT_AD_AVAILABLE_TIME, 0);
        long remaining = nextAdTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public long getRemainingTime() {
        long expiration = prefs.getLong(KEY_MAX_EXPIRATION, 0);
        long remaining = expiration - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public int getRemainingActivations() {
        long now = System.currentTimeMillis();
        long lastDate = prefs.getLong(KEY_LAST_ACTIVATION_DATE, 0);
        if (!isSameDay(now, lastDate)) {
            return MAX_DAILY_LIMIT;
        }
        return Math.max(0, MAX_DAILY_LIMIT - prefs.getInt(KEY_DAILY_ACTIVATIONS, 0));
    }

    public boolean isNextAdRewardedInterstitial() {
        long now = System.currentTimeMillis();
        long lastDate = prefs.getLong(KEY_LAST_ACTIVATION_DATE, 0);
        
        if (!isSameDay(now, lastDate)) {
            return false; // Reset effectively (0 is even) -> Video
        }
        
        int currentActivations = prefs.getInt(KEY_DAILY_ACTIVATIONS, 0);
        return currentActivations % 2 != 0;
    }

    private boolean isSameDay(long time1, long time2) {
        // Simple day check using division (not perfect for timezones but sufficient for this logic usually)
        // Or better, use Calendar/LocalDate if available. Let's stick to simple day division for now
        // assuming UTC or consistent local time usage.
        // Actually, let's use a simpler approximation: 
        // If the difference is huge, it's definitely not same day. 
        // But to be precise for "Daily Limit", we usually mean "Calendar Day".
        // Let's use java.util.Calendar
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
