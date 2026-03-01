package com.radiodedios.gt.prayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PrayerGenerator {

    private static final String PREF_NAME = "prayer_history_prefs";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_LAST_PRAYER_INDEX = "last_prayer_index";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Random random;

    public PrayerGenerator(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.random = new Random();
    }

    public Prayer generatePrayer(String name, String category, String description) {
        String[] templates = getTemplatesForCategory(category);
        if (templates == null || templates.length == 0) {
            templates = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_general);
        }

        int lastIndex = prefs.getInt(KEY_LAST_PRAYER_INDEX + "_" + category, -1);
        int newIndex;
        if (templates.length > 1) {
            do {
                newIndex = random.nextInt(templates.length);
            } while (newIndex == lastIndex);
        } else {
            newIndex = 0;
        }

        prefs.edit().putInt(KEY_LAST_PRAYER_INDEX + "_" + category, newIndex).apply();

        String template = templates[newIndex];

        String defaultName = context.getString(com.radiodedios.gt.R.string.prayer_default_name);
        String defaultDesc = context.getString(com.radiodedios.gt.R.string.prayer_default_desc);
        String cleanName = (name != null && !name.trim().isEmpty()) ? name.trim() : defaultName;
        String cleanDesc = (description != null && !description.trim().isEmpty()) ? description.trim() : defaultDesc;

        String prayerText = String.format(template, cleanName, cleanDesc);

        String verse = getRandomVerseForCategory(category);

        Prayer prayer = new Prayer(
                System.currentTimeMillis(),
                category,
                prayerText,
                verse
        );

        saveToHistory(prayer);
        return prayer;
    }

    private int getCategoryIndex(String category) {
        String[] categories = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_categories);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(category)) {
                return i;
            }
        }
        return -1;
    }

    private String[] getTemplatesForCategory(String category) {
        int index = getCategoryIndex(category);
        switch (index) {
            case 0:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_health);
            case 1:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_economy);
            case 2:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_family);
            case 3:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_anxiety);
            case 4:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_strength);
            case 5:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_addictions);
            case 6:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_travel);
            default:
                return context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_templates_general);
        }
    }

    private String getRandomVerseForCategory(String category) {
        String[] verses;
        int index = getCategoryIndex(category);
        switch (index) {
            case 0:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_health);
                break;
            case 1:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_economy);
                break;
            case 2:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_family);
                break;
            case 3:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_anxiety);
                break;
            case 4:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_strength);
                break;
            case 5:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_addictions);
                break;
            case 6:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_travel);
                break;
            default:
                verses = context.getResources().getStringArray(com.radiodedios.gt.R.array.prayer_verses_general);
                break;
        }
        return verses[random.nextInt(verses.length)];
    }

    public void saveToHistory(Prayer prayer) {
        List<Prayer> history = getHistory();
        history.add(0, prayer); // Add to beginning
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    public List<Prayer> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Prayer>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
