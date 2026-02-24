package com.radiodedios.gt.manager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import com.radiodedios.gt.R;
import com.radiodedios.gt.model.RadioStation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DJModeManager {
    private static DJModeManager instance;
    private Context context;
    private TextToSpeech tts;
    private boolean isDJModeActive = false;
    private boolean isTtsReady = false;
    private MediaController mediaController;
    private List<RadioStation> stationList = new ArrayList<>();
    private RadioStation currentStation;
    private long stationStartTime = 0;

    // Threshold for changing station (e.g., 10 minutes)
    // For testing/demo purposes, we might want this lower, but for production:
    private static final long CHANGE_THRESHOLD_MS = 10 * 60 * 1000;
    private static final long CHECK_INTERVAL_MS = 60 * 1000; // Check every minute

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable monitorRunnable;

    public static synchronized DJModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new DJModeManager(context.getApplicationContext());
        }
        return instance;
    }

    private DJModeManager(Context context) {
        this.context = context;
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English
                    tts.setLanguage(Locale.US);
                }
                isTtsReady = true;

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if ("INTRO".equals(utteranceId)) {
                             // Intro finished, change station
                             handler.post(() -> performStationChange());
                        } else if ("OUTRO".equals(utteranceId)) {
                             // Outro finished, fade in
                             handler.post(() -> sendFadeIn());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {}
                });
            }
        });
    }

    public void setMediaController(MediaController mediaController) {
        this.mediaController = mediaController;
    }

    public void setStationList(List<RadioStation> list) {
        this.stationList = list;
    }

    public void onStationChanged(RadioStation station) {
        this.currentStation = station;
        this.stationStartTime = System.currentTimeMillis();
    }

    // Fallback if we don't have the object but just title
    public void onStationChanged(String stationName) {
        // Try to find in list
        if (stationList != null) {
            for (RadioStation s : stationList) {
                if (s.getName().equals(stationName)) {
                    this.currentStation = s;
                    break;
                }
            }
        }
        this.stationStartTime = System.currentTimeMillis();
    }

    public void toggleDJMode() {
        isDJModeActive = !isDJModeActive;
        if (isDJModeActive) {
            startMonitoring();
            Toast.makeText(context, R.string.dj_mode_active, Toast.LENGTH_SHORT).show();
        } else {
            stopMonitoring();
            Toast.makeText(context, R.string.dj_mode_inactive, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isActive() {
        return isDJModeActive;
    }

    private void startMonitoring() {
        if (monitorRunnable == null) {
            monitorRunnable = new Runnable() {
                @Override
                public void run() {
                    checkTime();
                    if (isDJModeActive) {
                        handler.postDelayed(this, CHECK_INTERVAL_MS);
                    }
                }
            };
        }
        handler.removeCallbacks(monitorRunnable);
        handler.postDelayed(monitorRunnable, CHECK_INTERVAL_MS);
    }

    private void stopMonitoring() {
        if (monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
        }
    }

    private void checkTime() {
        if (!isDJModeActive || mediaController == null || !mediaController.isPlaying()) return;
        if (currentStation == null) return;

        long diff = System.currentTimeMillis() - stationStartTime;
        if (diff > CHANGE_THRESHOLD_MS) {
            // Trigger Change
            startTransition();
            // Reset start time to prevent loops while transition happens (though transition will reset it)
            stationStartTime = System.currentTimeMillis();
        }
    }

    private void startTransition() {
        // 1. Fade Out
        sendFadeOut();

        // 2. Wait for fade (approx 2s) then Speak Intro
        handler.postDelayed(() -> {
            speak(context.getString(R.string.dj_intro_msg), "INTRO");
        }, 2500);
    }

    private void performStationChange() {
        if (stationList == null || stationList.isEmpty()) return;

        // Find candidate
        List<RadioStation> candidates = new ArrayList<>();
        String currentLang = currentStation != null ? currentStation.getLanguage() : null;

        for (RadioStation s : stationList) {
            if (s == currentStation) continue;
            if (currentLang != null && s.getLanguage() != null && s.getLanguage().equalsIgnoreCase(currentLang)) {
                 candidates.add(s);
            }
        }

        // If no language match, fallback to any other
        if (candidates.isEmpty()) {
            for (RadioStation s : stationList) {
                if (s != currentStation) candidates.add(s);
            }
        }

        if (candidates.isEmpty()) return; // Only 1 station exists

        RadioStation nextStation = candidates.get(new Random().nextInt(candidates.size()));

        // Change Station
        playStation(nextStation);

        // Update Current
        currentStation = nextStation;
        stationStartTime = System.currentTimeMillis();

        // Speak Outro
        String msg = String.format(context.getString(R.string.dj_outro_msg), nextStation.getName());
        speak(msg, "OUTRO");
    }

    private void playStation(RadioStation station) {
        if (mediaController == null) return;

        MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                .setTitle(station.getName())
                .setArtist(station.getDescription());

        if (station.getImage() != null && !station.getImage().isEmpty()) {
             metaBuilder.setArtworkUri(android.net.Uri.parse(station.getImage()));
        }

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(station.getStreamUrl())
                .setMediaMetadata(metaBuilder.build())
                .build();

        mediaController.setMediaItem(mediaItem);
        mediaController.prepare();
        mediaController.play();
    }

    private void speak(String text, String id) {
        if (isTtsReady && tts != null) {
            // Use QUEUE_ADD so it plays after any current speech,
            // but here we control flow via ID callbacks.
            // Using QUEUE_FLUSH to be safe and immediate.
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
        } else {
            // If TTS fails, manually proceed
            if ("INTRO".equals(id)) performStationChange();
            if ("OUTRO".equals(id)) sendFadeIn();
        }
    }

    private void sendFadeOut() {
        if (mediaController != null) {
            SessionCommand command = new SessionCommand("ACTION_DJ_FADE_OUT", new Bundle());
            mediaController.sendCustomCommand(command, Bundle.EMPTY);
        }
    }

    private void sendFadeIn() {
        if (mediaController != null) {
            SessionCommand command = new SessionCommand("ACTION_DJ_FADE_IN", new Bundle());
            mediaController.sendCustomCommand(command, Bundle.EMPTY);
        }
    }
}
