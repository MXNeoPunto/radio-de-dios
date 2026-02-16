package com.radiodedios.gt.model;

import com.google.gson.annotations.SerializedName;

public class AppInfo {
    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("updated")
    private String updated;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getUpdated() {
        return updated;
    }
}
