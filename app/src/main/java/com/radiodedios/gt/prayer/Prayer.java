package com.radiodedios.gt.prayer;

public class Prayer {
    private long timestamp;
    private String category;
    private String text;
    private String verse;

    public Prayer(long timestamp, String category, String text, String verse) {
        this.timestamp = timestamp;
        this.category = category;
        this.text = text;
        this.verse = verse;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public String getText() {
        return text;
    }

    public String getVerse() {
        return verse;
    }
}
