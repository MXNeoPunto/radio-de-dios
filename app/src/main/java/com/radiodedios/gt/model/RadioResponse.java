package com.radiodedios.gt.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RadioResponse {
    @SerializedName("app")
    private AppInfo app;

    @SerializedName("radios")
    private List<RadioStation> radios;

    @SerializedName("banner")
    private BannerConfig banner;

    public AppInfo getApp() {
        return app;
    }

    public List<RadioStation> getRadios() {
        return radios;
    }

    public BannerConfig getBannerConfig() {
        return banner;
    }

    public static class BannerConfig {
        @SerializedName("enabled")
        private boolean enabled;

        @SerializedName("image")
        private String image;

        public boolean isEnabled() {
            return enabled;
        }

        public String getImage() {
            return image;
        }
    }
}
