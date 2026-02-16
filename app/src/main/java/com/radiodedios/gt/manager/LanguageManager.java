package com.radiodedios.gt.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LanguageManager {
    private static final String PREF_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";
    
    private final SharedPreferences prefs;

    public LanguageManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLanguage(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public String getLanguage() {
        if (!prefs.contains(KEY_LANGUAGE)) {
            String systemLang = Locale.getDefault().getLanguage();
            if (systemLang != null && systemLang.equals("es")) {
                return "es";
            } else {
                return "en";
            }
        }
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public Context applyLanguage(Context context) {
        String languageCode = getLanguage();
        Locale locale = new Locale.Builder().setLanguage(languageCode).build();
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }
}
