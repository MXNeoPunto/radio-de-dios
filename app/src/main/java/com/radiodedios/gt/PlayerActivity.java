package com.radiodedios.gt;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.radiodedios.gt.ui.RGBWaveView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.graphics.drawable.Drawable;
import android.graphics.Color;

public class PlayerActivity extends AppCompatActivity {

    private MediaController mediaController;
    private View btnPlayPauseContainer;
    private ImageView btnPlayPauseIcon;
    private ImageView playerImage;
    private TextView title, desc;
    private android.widget.ImageButton btnClose;
    private android.widget.ImageButton btnShare;
    private View btnSleepTimerContainer; // Use container click
    private android.widget.ImageButton btnSleepTimerIcon;
    private android.widget.TextView tvTimerCountdown;
    private android.widget.ImageButton btnCarMode;
    private com.radiodedios.gt.manager.AdsManager adsManager;
    private BillingManager billingManager;
    private SleepTimerManager sleepTimerManager;
    private View rootLayout;

    private ThemeManager themeManager;
    private com.radiodedios.gt.manager.LanguageManager languageManager;
    private RGBWaveView waveView;

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

        rootLayout = findViewById(R.id.playerRoot);
        
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        btnPlayPauseContainer = findViewById(R.id.btnPlayPauseContainer);
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon);
        title = findViewById(R.id.playerTitle);
        desc = findViewById(R.id.playerDesc);
        playerImage = findViewById(R.id.playerImage);
        btnClose = findViewById(R.id.btnClose);
        btnShare = findViewById(R.id.btnShare);
        
        // Sleep Timer
        btnSleepTimerContainer = findViewById(R.id.sleepTimerContainer);
        btnSleepTimerIcon = findViewById(R.id.btnSleepTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        
        btnCarMode = findViewById(R.id.btnCarMode);
        waveView = findViewById(R.id.waveView);
        
        billingManager = new BillingManager(this);
        adsManager = new com.radiodedios.gt.manager.AdsManager(this, billingManager);
        sleepTimerManager = SleepTimerManager.getInstance();

        // Restore timer state
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
            private float initialY;
            private boolean isDragging = false;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        initialY = v.getTranslationY();
                        isDragging = false;
                        return true;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startY;
                        if (deltaY > 0) { // Only drag downwards
                            v.setTranslationY(deltaY);
                            // Optional: Scale down slightly as we drag
                            float scale = 1.0f - (deltaY / v.getHeight() * 0.2f); // Max 20% shrink
                            v.setScaleX(Math.max(0.8f, scale));
                            v.setScaleY(Math.max(0.8f, scale));
                            isDragging = true;
                        }
                        return true;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (isDragging) {
                            float currentY = v.getTranslationY();
                            if (currentY > v.getHeight() * 0.25f) { // Dismiss if dragged 25% down
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
                                // Bounce back
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
        btnPlayPauseIcon.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        if (waveView != null) {
            if (isPlaying) {
                waveView.startAnimation();
            } else {
                waveView.stopAnimation();
            }
        }
    }
    
    private void updateMetadata(MediaMetadata metadata) {
        if (isFinishing() || isDestroyed()) return;

        if (metadata != null) {
            title.setText(metadata.title != null ? metadata.title : "Unknown");
            desc.setText(metadata.artist != null ? metadata.artist : "");

            int color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
            Drawable placeholder = ContextCompat.getDrawable(this, R.drawable.ic_music_placeholder);
            Drawable tintedPlaceholder = null;
            if (placeholder != null) {
                tintedPlaceholder = DrawableCompat.wrap(placeholder).mutate();
                DrawableCompat.setTint(tintedPlaceholder, color);
            }

            if (metadata.artworkUri != null) {
                playerImage.clearColorFilter();
                Glide.with(this)
                        .load(metadata.artworkUri)
                        .placeholder(tintedPlaceholder)
                        .error(tintedPlaceholder)
                        .into(playerImage);
            } else {
                playerImage.setImageDrawable(tintedPlaceholder);
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
