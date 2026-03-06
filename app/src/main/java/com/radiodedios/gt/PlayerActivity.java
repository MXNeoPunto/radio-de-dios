package com.radiodedios.gt;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.radiodedios.gt.manager.SleepTimerManager;
import com.radiodedios.gt.manager.ThemeManager;
import com.radiodedios.gt.manager.BillingManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Animatable;
import android.graphics.Color;

public class PlayerActivity extends AppCompatActivity {

    private MediaController mediaController;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnPlayPauseContainer;
    private TextView title, desc, nextSong;
    private ImageButton btnClose;
    private ImageButton btnShare;
    private View btnSleepTimerContainer;
    private ImageButton btnSleepTimerIcon;
    private TextView tvTimerCountdown;
    private ImageButton btnCarMode;

    // New Features
    private ToneGenerator toneGenerator;

    private com.radiodedios.gt.manager.AdsManager adsManager;
    private BillingManager billingManager;
    private SleepTimerManager sleepTimerManager;
    private View rootLayout;

    private ThemeManager themeManager;
    private com.radiodedios.gt.manager.LanguageManager languageManager;
    private com.google.android.material.imageview.ShapeableImageView stationArtwork;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        com.radiodedios.gt.manager.LanguageManager langMgr = new com.radiodedios.gt.manager.LanguageManager(newBase);
        super.attachBaseContext(langMgr.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        languageManager = new com.radiodedios.gt.manager.LanguageManager(this);
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        rootLayout = findViewById(R.id.playerRoot);
        
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        btnPlayPauseContainer = findViewById(R.id.btnPlayPauseContainer);
        title = findViewById(R.id.playerTitle);
        desc = findViewById(R.id.playerDesc);
        nextSong = findViewById(R.id.playerNextSong);
        btnClose = findViewById(R.id.btnClose);
        btnShare = findViewById(R.id.btnShare);
        
        btnSleepTimerContainer = findViewById(R.id.sleepTimerContainer);
        btnSleepTimerIcon = findViewById(R.id.btnSleepTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        
        btnCarMode = findViewById(R.id.btnCarMode);
        stationArtwork = findViewById(R.id.stationArtwork);

        billingManager = new BillingManager(this);
        adsManager = new com.radiodedios.gt.manager.AdsManager(this, billingManager);
        sleepTimerManager = SleepTimerManager.getInstance();

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
                        finish();
                    }
                }
            });
            updateTimerUI(sleepTimerManager.getRemainingTimeMillis());
        }

        btnClose.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
            startActivity(Intent.createChooser(intent, getString(R.string.share_app)));
        });

        btnSleepTimerContainer.setOnClickListener(v -> showSleepTimerDialog());

        btnCarMode.setOnClickListener(v -> {
            startActivity(new Intent(this, CarModeActivity.class));
        });
        
        setupSwipeToDismiss();

        btnPlayPauseContainer.setOnClickListener(v -> {
            animateButton(v);
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });
        
        setupMediaController();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sleepTimerManager != null && sleepTimerManager.isTimerRunning()) {
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
                        finish();
                    }
                }
            });
            updateTimerUI(sleepTimerManager.getRemainingTimeMillis());
        } else {
            updateTimerUI(0);
        }
    }


    private void animateButton(View view) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .setDuration(200)
                    .start();
            })
            .start();
    }

    @SuppressWarnings("deprecation")
    private void setupSwipeToDismiss() {
        rootLayout.setOnTouchListener(new android.view.View.OnTouchListener() {
            private float startY;
            private boolean isDragging = false;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        isDragging = false;
                        return true;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startY;
                        if (deltaY > 0) {
                            v.setTranslationY(deltaY);
                            float scale = 1.0f - (deltaY / v.getHeight() * 0.2f);
                            v.setScaleX(Math.max(0.8f, scale));
                            v.setScaleY(Math.max(0.8f, scale));
                            isDragging = true;
                        }
                        return true;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (isDragging) {
                            float currentY = v.getTranslationY();
                            if (currentY > v.getHeight() * 0.25f) {
                                v.animate()
                                    .translationY(v.getHeight())
                                    .alpha(0f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        finish();
                                        if (android.os.Build.VERSION.SDK_INT >= 34) {
                                            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
                                        } else {
                                            overridePendingTransition(0, 0);
                                        }
                                    })
                                    .start();
                            } else {
                                v.animate()
                                    .translationY(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(300)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                                    .start();
                            }
                        } else if (Math.abs(event.getRawY() - startY) < 10) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
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
                    
                    @Override
                    public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                        updateMetadata(mediaMetadata);
                    }
                });
                
                updateUI(mediaController.isPlaying());
                updateMetadata(mediaController.getMediaMetadata());
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateUI(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPauseContainer.setImageResource(R.drawable.avd_play_to_pause);
        } else {
            btnPlayPauseContainer.setImageResource(R.drawable.avd_pause_to_play);
        }

        Drawable drawable = btnPlayPauseContainer.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }
    }
    
    private void updateMetadata(MediaMetadata metadata) {
        if (isFinishing() || isDestroyed()) return;

        if (metadata != null) {
            String titleText = metadata.title != null ? metadata.title.toString() : "";
            String artistText = metadata.artist != null ? metadata.artist.toString() : "";

            if (titleText.trim().isEmpty()) {
                titleText = artistText;
                artistText = "";
            }
            if (titleText.trim().isEmpty()) {
                titleText = "Unknown";
            }

            title.setText(titleText);
            desc.setText(artistText);

            // Try to extract extra info if stream supports "Next Song" or similar custom metadata.
            // Often custom metadata is placed in `description` or `subtitle`.
            // In RadioService we map standard title to title.
            // In standard shoutcast/icy streams, "next song" is rarely natively supported without custom JSON polling,
            // but we add UI support for it via standard metadata fields if available in future updates.
            if (metadata.subtitle != null && !metadata.subtitle.toString().isEmpty()) {
                nextSong.setVisibility(View.VISIBLE);
                nextSong.setText("Next: " + metadata.subtitle);
            } else {
                nextSong.setVisibility(View.GONE);
            }

            if (metadata.artworkUri != null) {
                Glide.with(this)
                     .load(metadata.artworkUri)
                     .placeholder(R.drawable.ic_music_placeholder)
                     .error(R.drawable.ic_music_placeholder)
                     .into(stationArtwork);
            } else {
                stationArtwork.setImageResource(R.drawable.ic_music_placeholder);
            }
        }
    }

    private void showSleepTimerDialog() {
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
                                finish();
                            }
                        }
                    });
                    android.widget.Toast.makeText(this, getString(R.string.sleep_timer_set, min), android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    sleepTimerManager.cancelTimer();
                    updateTimerUI(0);
                }
            })
            .show();
    }

    private void updateTimerUI(long millisUntilFinished) {
        runOnUiThread(() -> {
            if (millisUntilFinished > 0) {
                btnSleepTimerIcon.setAlpha(1.0f);
                tvTimerCountdown.setVisibility(View.VISIBLE);
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimerCountdown.setText(String.format("%d:%02d", minutes, seconds));
            } else {
                btnSleepTimerIcon.setAlpha(0.6f);
                tvTimerCountdown.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        if (sleepTimerManager != null) {
             sleepTimerManager.setListener(null);
        }
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
        super.onDestroy();
    }
}
