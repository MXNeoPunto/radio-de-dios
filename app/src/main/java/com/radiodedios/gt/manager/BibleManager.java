package com.radiodedios.gt.manager;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.radiodedios.gt.R;

public class BibleManager {
    private static final String PREF_NAME = "bible_prefs";
    private static final String KEY_LAST_DAY = "last_day_of_year";
    private static final String KEY_TODAY_VERSE_INDEX = "today_verse_index";
    
    private final SharedPreferences prefs;
    private final Context context;

    public BibleManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getDailyVerse() {
        String[] verses = context.getResources().getStringArray(R.array.verses);
        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int lastDay = prefs.getInt(KEY_LAST_DAY, -1);
        int index;

        if (today != lastDay) {
            index = new Random().nextInt(verses.length);
            prefs.edit()
                .putInt(KEY_LAST_DAY, today)
                .putInt(KEY_TODAY_VERSE_INDEX, index)
                .apply();
        } else {
            index = prefs.getInt(KEY_TODAY_VERSE_INDEX, 0);
        }
        
        if (index >= 0 && index < verses.length) {
            return verses[index];
        }
        return verses[0];
    }
}
