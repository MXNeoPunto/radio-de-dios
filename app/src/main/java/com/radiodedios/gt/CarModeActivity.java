package com.radiodedios.gt;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.radiodedios.gt.manager.LanguageManager;
import com.radiodedios.gt.manager.SleepTimerManager;
import com.radiodedios.gt.manager.ThemeManager;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class CarModeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    
    private MediaController mediaController;
    private Button btnPlayPause;
    private TextView tvBlessing;
    private TextView tvTripTimer;
    private TextToSpeech tts;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable speechRunnable;
    private SleepTimerManager sleepTimerManager;
    private boolean isTtsReady = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        LanguageManager langMgr = new LanguageManager(newBase);
        super.attachBaseContext(langMgr.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ThemeManager(this).applyTheme();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_car_mode);

        Button btnClose = findViewById(R.id.btnExitCarMode);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        Button btnQuickFavorite = findViewById(R.id.btnQuickFavorite);
        tvBlessing = findViewById(R.id.tvBlessing);
        tvTripTimer = findViewById(R.id.tvTripTimer);
        
        btnClose.setOnClickListener(v -> finish());
        
        btnPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        btnQuickFavorite.setOnClickListener(v -> playQuickFavorite());
        
        // Show Blessing
        showBlessing();

        sleepTimerManager = SleepTimerManager.getInstance();
        tvTripTimer.setOnClickListener(v -> showTimerDialog());
        updateTimerUI(sleepTimerManager.getRemainingTimeMillis());

        // Init TTS
        tts = new TextToSpeech(this, this);

        setupMediaController();
    }

    private void showBlessing() {
        String[] verses = getResources().getStringArray(R.array.verses);
        if (verses.length > 0) {
            String randomVerse = verses[new Random().nextInt(verses.length)];
            tvBlessing.setText(randomVerse);
        } else {
            tvBlessing.setText(getString(R.string.blessing_trip));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check favorites
        SharedPreferences prefs = getSharedPreferences("favorite_prefs", MODE_PRIVATE);
        Set<String> favorites = prefs.getStringSet("pinned_urls", null);
        Button btnQuickFavorite = findViewById(R.id.btnQuickFavorite);
        if (favorites == null || favorites.isEmpty()) {
            btnQuickFavorite.setVisibility(View.GONE);
        } else {
            btnQuickFavorite.setVisibility(View.VISIBLE);
        }

        if (sleepTimerManager.isTimerRunning()) {
            sleepTimerManager.setListener(new SleepTimerManager.TimerListener() {
                @Override
                public void onTick(long millisUntilFinished) {
                    updateTimerUI(millisUntilFinished);
                }
                @Override
                public void onFinish() {
                    updateTimerUI(0);
                    if (mediaController != null) {
                        mediaController.pause();
                    }
                    finish();
                }
            });
            updateTimerUI(sleepTimerManager.getRemainingTimeMillis());
        } else {
            updateTimerUI(0);
        }
    }

    private void showTimerDialog() {
        if (sleepTimerManager.isTimerRunning()) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sleep_timer)
                .setMessage(getString(R.string.cancel_sleep_timer_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    sleepTimerManager.cancelTimer();
                    updateTimerUI(0);
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
            return;
        }

        String[] options = {"15 min", "30 min", "45 min", "60 min", getString(R.string.cancel)};
        int[] minutes = {15, 30, 45, 60, 0};

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sleep_timer)
            .setItems(options, (dialog, which) -> {
                if (which < 4) {
                    int min = minutes[which];
                    sleepTimerManager.startTimer(this, min, new SleepTimerManager.TimerListener() {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            updateTimerUI(millisUntilFinished);
                        }

                        @Override
                        public void onFinish() {
                            updateTimerUI(0);
                            if (mediaController != null) {
                                mediaController.pause();
                            }
                            finish();
                        }
                    });
                    Toast.makeText(this, getString(R.string.sleep_timer_set, min), Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void updateTimerUI(long millisUntilFinished) {
        runOnUiThread(() -> {
            if (millisUntilFinished > 0) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTripTimer.setText(String.format("%d:%02d", minutes, seconds));
                tvTripTimer.setAlpha(1.0f);
            } else {
                tvTripTimer.setText(getString(R.string.timer_idle));
                tvTripTimer.setAlpha(0.6f);
            }
        });
    }

    private void playQuickFavorite() {
        SharedPreferences prefs = getSharedPreferences("favorite_prefs", MODE_PRIVATE);
        Set<String> favorites = prefs.getStringSet("pinned_urls", null);

        if (favorites != null && !favorites.isEmpty()) {
            String url = favorites.iterator().next(); // Get first one
            if (mediaController != null) {
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle("Favorite Station")
                        .build();
                MediaItem item = new MediaItem.Builder()
                        .setUri(url)
                        .setMediaMetadata(metadata)
                        .build();
                mediaController.setMediaItem(item);
                mediaController.prepare();
                mediaController.play();
                Toast.makeText(this, R.string.quick_favorite, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.no_favorites_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, RadioService.class));
        ListenableFuture<MediaController> controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updateUI(isPlaying);
                    }
                });
                updateUI(mediaController.isPlaying());

                // Start speaking loop
                startSpeakingLoop();

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateUI(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setText(getString(R.string.pause));
        } else {
            btnPlayPause.setText(getString(R.string.play));
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Log error
            } else {
                isTtsReady = true;
            }
        }
    }

    private void startSpeakingLoop() {
        speechRunnable = new Runnable() {
            @Override
            public void run() {
                speakCurrentStation();
                // Repeat every 5 minutes
                handler.postDelayed(this, 5 * 60 * 1000);
            }
        };
        // Initial delay 10 seconds
        handler.postDelayed(speechRunnable, 10000);
    }

    private void speakCurrentStation() {
        if (isTtsReady && mediaController != null && mediaController.isPlaying()) {
            MediaMetadata metadata = mediaController.getMediaMetadata();
            if (metadata != null && metadata.title != null) {
                String text = getString(R.string.listening_to, metadata.title);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "StationInfo");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (handler != null && speechRunnable != null) {
            handler.removeCallbacks(speechRunnable);
        }
        // mediaController is managed by Future, usually we don't need to release it here if we didn't create the session,
        // but MediaController.release() disconnects.
        if (mediaController != null) {
            mediaController.release();
        }
        super.onDestroy();
    }
}
