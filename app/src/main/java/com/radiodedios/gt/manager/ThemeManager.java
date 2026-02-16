package com.radiodedios.gt.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;

public class ThemeManager {
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private final SharedPreferences prefs;
    private final Context context;

    public ThemeManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void applyTheme() {
        int theme = prefs.getInt(KEY_THEME, THEME_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
        
        // Apply Dynamic Colors if available (Material 3)
        if (DynamicColors.isDynamicColorAvailable()) {
            if (context.getApplicationContext() instanceof android.app.Application) {
                DynamicColors.applyToActivitiesIfAvailable((android.app.Application) 
                    context.getApplicationContext());
            }
        }
    }

    public void setTheme(int theme) {
        prefs.edit().putInt(KEY_THEME, theme).apply();
        AppCompatDelegate.setDefaultNightMode(theme);
    }

    public int getCurrentTheme() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

}
