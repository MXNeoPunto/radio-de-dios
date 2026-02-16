package com.radiodedios.gt.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class RadioStation implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("image")
    private String image;

    @SerializedName("stream_url")
    private String streamUrl;

    @SerializedName("description")
    private String description;

    @SerializedName("popular")
    private boolean popular;

    @SerializedName("language")
    private String language;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPopular() {
        return popular;
    }

    public String getLanguage() {
        return language;
    }
}
