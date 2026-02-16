package com.radiodedios.gt.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RadioResponse {
    @SerializedName("app")
    private AppInfo app;

    @SerializedName("radios")
    private List<RadioStation> radios;

    public AppInfo getApp() {
        return app;
    }

    public List<RadioStation> getRadios() {
        return radios;
    }
}
