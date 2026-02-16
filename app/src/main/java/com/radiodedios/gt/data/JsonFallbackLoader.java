package com.radiodedios.gt.data;

import android.content.Context;
import com.google.gson.Gson;
import com.radiodedios.gt.model.RadioResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonFallbackLoader {

    
    private static final int[] ENCRYPTED_URL = {
        59, 49, 55, 34, 54, 110, 112, 100, 43, 60, 48, 34, 52, 42, 61, 32, 
        113, 36, 59, 62, 106, 48, 38, 55, 49, 62, 38, 107, 51, 44, 61, 47
    };
    private static final String ENCRYPTION_KEY = "SECRET_KEY_RADIO_GT";
    
    private static final String LOCAL_FILE = "radio_station.json";
    private final Context context;
    private final Gson gson;
    private final ExecutorService executor;

    public interface Callback {
        void onSuccess(RadioResponse response);
        void onError(Exception e);
    }

    public JsonFallbackLoader(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    private String getDecryptedUrl() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ENCRYPTED_URL.length; i++) {
            sb.append((char) (ENCRYPTED_URL[i] ^ ENCRYPTION_KEY.charAt(i % ENCRYPTION_KEY.length())));
        }
        return sb.toString();
    }

    public void loadData(Callback callback) {
        executor.execute(() -> {
            try {
                // Try loading from online
                String json = loadFromUrl(getDecryptedUrl());
                if (json == null) {
                    throw new IOException("Failed to load from URL");
                }
                RadioResponse response = gson.fromJson(json, RadioResponse.class);
                callback.onSuccess(response);
            } catch (Exception e) {
                // Fallback to local assets
                try {
                    String json = loadFromAssets(LOCAL_FILE);
                    RadioResponse response = gson.fromJson(json, RadioResponse.class);
                    callback.onSuccess(response);
                } catch (IOException ex) {
                    callback.onError(ex);
                }
            }
        });
    }

    public void loadOnlineOnly(Callback callback) {
        executor.execute(() -> {
             try {
                String json = loadFromUrl(getDecryptedUrl());
                if (json == null) {
                     throw new IOException("Failed to load from URL");
                }
                RadioResponse response = gson.fromJson(json, RadioResponse.class);
                callback.onSuccess(response);
             } catch (Exception e) {
                 callback.onError(e);
             }
        });
    }

    private String loadFromUrl(String urlString) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            
            if (urlConnection.getResponseCode() == 200) {
                 return readStream(urlConnection.getInputStream());
            }
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private String loadFromAssets(String filename) throws IOException {
        return readStream(context.getAssets().open(filename));
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }
}
