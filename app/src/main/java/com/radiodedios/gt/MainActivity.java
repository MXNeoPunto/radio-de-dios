package com.radiodedios.gt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.radiodedios.gt.data.JsonFallbackLoader;
import com.radiodedios.gt.manager.AdsManager;
import com.radiodedios.gt.manager.BillingManager;
import com.radiodedios.gt.manager.BibleManager;
import com.radiodedios.gt.manager.HistoryManager;
import com.radiodedios.gt.manager.LanguageManager;
import com.radiodedios.gt.manager.MaxManager;
import com.radiodedios.gt.manager.ThemeManager;
import com.radiodedios.gt.model.RadioResponse;
import com.radiodedios.gt.model.RadioStation;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import android.content.ComponentName;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import androidx.media3.common.Player;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.color.MaterialColors;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.graphics.drawable.Drawable;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity {

    private AdsManager adsManager;
    private MaxManager maxManager;
    private BillingManager billingManager;
    private ThemeManager themeManager;
    private LanguageManager languageManager;
    private BibleManager bibleManager;
    private HistoryManager historyManager;
    private RecyclerView recyclerView;
    private AdView adView;
    private FloatingActionButton fabResume;
    private View loadingIndicator;
    private View miniPlayerContainer;
    private View miniPlayerCard;
    private ImageView miniPlayerImage;
    private TextView miniPlayerTitle;
    private View miniPlayerPlayPause;
    private ImageView miniPlayerPlayPauseIcon;
    private View bottomContainer;

    private MediaController mediaController;
    private boolean wasPlayingBeforeAd = false;
    private long lastToastTime = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        LanguageManager langMgr = new LanguageManager(newBase);
        super.attachBaseContext(langMgr.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        languageManager = new LanguageManager(this);
        bibleManager = new BibleManager(this);
        historyManager = new HistoryManager(this);
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bottomContainer = findViewById(R.id.bottomContainer);
        ViewCompat.setOnApplyWindowInsetsListener(bottomContainer, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        billingManager = new BillingManager(this);
        billingManager.setCallback(new BillingManager.BillingCallback() {
            @Override
            public void onPurchaseSuccess() {
                Toast.makeText(MainActivity.this, R.string.premium_active, Toast.LENGTH_LONG).show();
                adView.setVisibility(View.GONE);
            }

            @Override
            public void onPurchaseFailure(int messageId) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastToastTime > 2000) {
                    lastToastTime = currentTime;
                    Toast.makeText(MainActivity.this, getString(messageId), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPremiumChecked(boolean isPremium) {
                runOnUiThread(() -> {
                    if (adsManager != null && adView != null) {
                        adsManager.loadBanner(adView);
                    }
                });
            }
        });

        adsManager = new AdsManager(this, billingManager);
        maxManager = new MaxManager(this);

        recyclerView = findViewById(R.id.recyclerView);
        setupGridLayout();

        // Dynamic padding for RecyclerView to avoid overlapping with mini player
        recyclerView.setClipToPadding(false);
        bottomContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = v.getHeight();
            if (height != recyclerView.getPaddingBottom()) {
                recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                        recyclerView.getPaddingRight(), height);
            }
        });

        adView = findViewById(R.id.adView);
        fabResume = findViewById(R.id.fabResume);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        miniPlayerContainer = findViewById(R.id.miniPlayerContainer);
        miniPlayerCard = findViewById(R.id.miniPlayerImageContainer);
        miniPlayerImage = findViewById(R.id.miniPlayerImage);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerPlayPauseIcon = (ImageView) miniPlayerPlayPause;

        // Load Ads
        adsManager.loadBanner(adView);
        // Interstitial might be shown later

        // Load Data
        loadData();

        // Check Battery Optimization
        BatteryOptimizationHelper.checkBatteryOptimization(this);

        // Rate Dialog
        checkRateDialog();

        // Notification Permission
        checkNotificationPermission();

        // Connect to MediaService
        setupMediaController();
        
        miniPlayerPlayPause.setOnClickListener(v -> {
            animateButton(v);
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });
        
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                miniPlayerContainer.performClick();
                return true;
            }

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffY = e2.getY() - e1.getY();
                    if (diffY < -50) { // Swipe Up
                        miniPlayerContainer.performClick();
                        return true;
                    }
                }
                return false;
            }
        });

        miniPlayerContainer.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        miniPlayerContainer.setOnClickListener(v -> {
            adsManager.showInterstitial(this,
                this::pauseRadio,
                () -> {
                    if (wasPlayingBeforeAd && mediaController != null && !mediaController.isPlaying()) {
                        mediaController.play();
                    }
                    // Open Full Player (Animated)
                    Intent intent = new Intent(this, PlayerActivity.class);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this, miniPlayerCard, "player_image");
                    startActivity(intent, options.toBundle());
                });
        });
        
        fabResume.setOnClickListener(v -> {
             animateButton(v);
             playLastStation();
             fabResume.setVisibility(View.GONE);
        });

        // checkResumeLastStation(); // Moved to MediaController callback
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

    private void checkResumeLastStation() {
        if (historyManager.hasLastStation()) {
            // Only show if NOT playing
            if (mediaController != null && mediaController.isPlaying()) {
                return;
            }
            // If mediaController is null (async), we check inside the listener, 
            // but here we are in onCreate.
            // Actually, we should check this logic AFTER mediaController is connected.
        }
    }

    private void playLastStation() {
        if (mediaController != null && historyManager.getLastStationUrl() != null) {
             androidx.media3.common.MediaMetadata.Builder metaBuilder = new androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(historyManager.getLastStationName());

             String imgUrl = historyManager.getLastStationImg();
             if (imgUrl != null && !imgUrl.isEmpty()) {
                 metaBuilder.setArtworkUri(Uri.parse(imgUrl));
             }

             androidx.media3.common.MediaMetadata metadata = metaBuilder.build();

             androidx.media3.common.MediaItem mediaItem = new androidx.media3.common.MediaItem.Builder()
                        .setUri(historyManager.getLastStationUrl())
                        .setMediaMetadata(metadata)
                        .build();
                        
             mediaController.setMediaItem(mediaItem);
             mediaController.prepare();
             mediaController.play();
             
             Intent intent = new Intent(this, PlayerActivity.class);
             startActivity(intent);
        }
    }

    private void setupGridLayout() {
        int spanCount = 2;
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp >= 600) {
            spanCount = 3;
        }
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
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
                        updateMiniPlayerState(isPlaying);
                    }
                    
                    @Override
                    public void onMediaMetadataChanged(androidx.media3.common.MediaMetadata mediaMetadata) {
                        updateMiniPlayerMetadata(mediaMetadata);
                    }
                });
                
                // Initial state
                if (mediaController.getMediaMetadata() != null) {
                    updateMiniPlayerMetadata(mediaController.getMediaMetadata());
                }
                updateMiniPlayerState(mediaController.isPlaying());
                
                // Check Resume ONLY if not playing
                if (!mediaController.isPlaying() && historyManager.hasLastStation()) {
                    fabResume.setVisibility(View.VISIBLE);
                }
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateMiniPlayerMetadata(androidx.media3.common.MediaMetadata mediaMetadata) {
        if (isFinishing() || isDestroyed()) return;

        if (mediaMetadata.title != null) {
            miniPlayerTitle.setText(mediaMetadata.title);
            miniPlayerContainer.setVisibility(View.VISIBLE);
        } else if (miniPlayerContainer.getVisibility() == View.VISIBLE && mediaMetadata.artworkUri == null) {
            // Keep visible if it was already visible? Or maybe we don't need this else.
            // Just updating title if present.
        }

        int color = MaterialColors.getColor(MainActivity.this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
        Drawable placeholder = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_music_placeholder);
        Drawable tintedPlaceholder = null;
        if (placeholder != null) {
            tintedPlaceholder = DrawableCompat.wrap(placeholder).mutate();
            DrawableCompat.setTint(tintedPlaceholder, color);
        }

        if (mediaMetadata.artworkUri != null) {
            miniPlayerImage.clearColorFilter();
            Glide.with(MainActivity.this)
                    .load(mediaMetadata.artworkUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(tintedPlaceholder)
                    .error(tintedPlaceholder)
                    .into(miniPlayerImage);
        } else {
            // Only clear if we are actually updating metadata and it's missing artwork, 
            // but usually we want to keep previous if playing? 
            // Actually, if artworkUri is null here, it means the current item has no artwork.
            miniPlayerImage.setImageDrawable(tintedPlaceholder);
        }
    }

    private void updateMiniPlayerState(boolean isPlaying) {
        if (isPlaying) {
            fabResume.setVisibility(View.GONE);
            miniPlayerPlayPauseIcon.setImageResource(R.drawable.ic_pause);
        } else {
            miniPlayerPlayPauseIcon.setImageResource(R.drawable.ic_play);
        }
    }

    private void loadData() {
        loadingIndicator.setVisibility(View.VISIBLE);
        new JsonFallbackLoader(this).loadData(new JsonFallbackLoader.Callback() {
            @Override
            public void onSuccess(RadioResponse response) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    List<RadioStation> allRadios = response.getRadios();
                    List<RadioStation> filteredRadios = new ArrayList<>();
                    String currentLang = languageManager.getLanguage();

                    if (allRadios != null) {
                        for (RadioStation station : allRadios) {
                            if (station.getLanguage() != null && station.getLanguage().equalsIgnoreCase(currentLang)) {
                                filteredRadios.add(station);
                            }
                        }
                    }

                    // Load pinned status
                    android.content.SharedPreferences prefs = getSharedPreferences("pinned_prefs", MODE_PRIVATE);
                    java.util.Set<String> pinnedSet = prefs.getStringSet("pinned_urls", new java.util.HashSet<>());
                    if (pinnedSet != null) {
                        for (RadioStation station : filteredRadios) {
                            if (pinnedSet.contains(station.getStreamUrl())) {
                                station.setPinned(true);
                            }
                        }
                    }

                    // Initial Sort
                    java.util.Collections.sort(filteredRadios, (r1, r2) -> {
                        boolean p1 = r1.isPinned();
                        boolean p2 = r2.isPinned();
                        if (p1 && !p2) return -1;
                        if (!p1 && p2) return 1;
                        return 0;
                    });

                    setupList(filteredRadios);

                    if (response.getBannerConfig() != null) {
                        checkAndShowInterstitial(response.getBannerConfig());
                    }

                    Toast.makeText(MainActivity.this, R.string.station_list_updated, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.error_loading_data, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkAndShowInterstitial(RadioResponse.BannerConfig config) {
        if (!config.isEnabled() || config.getImage() == null || config.getImage().isEmpty()) {
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastShown = prefs.getLong("pref_interstitial_last_shown", 0);
        long currentTime = System.currentTimeMillis();

        // 24 hours in milliseconds = 86400000
        if (currentTime - lastShown < 86400000) {
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_interstitial);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView image = dialog.findViewById(R.id.interstitialImage);
        View btnClose = dialog.findViewById(R.id.btnClose);

        Glide.with(this)
                .load(config.getImage())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(image);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            prefs.edit().putLong("pref_interstitial_last_shown", System.currentTimeMillis()).apply();
        });

        dialog.show();
    }

    private void setupList(List<RadioStation> radios) {
        recyclerView.setAdapter(new RadioAdapter(radios));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        MenuItem item = menu.findItem(R.id.action_notifications);
        if (item != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String lastSeenDate = prefs.getString("verse_seen_date", "");
            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

            if (!currentDate.equals(lastSeenDate)) {
                item.setIcon(R.drawable.ic_notification_active);
            } else {
                item.setIcon(R.drawable.ic_notification); // Assuming this is the default icon name
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_max) {
            handleMaxAction();
            return true;
        } else if (id == R.id.action_notifications) {
            startActivity(new Intent(this, com.radiodedios.gt.ui.NotificationHistoryActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            // Show theme dialog or activity
            showThemeDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        } else if (id == R.id.action_privacy) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.e-droid.net/privacy.php?ida=3019300&idl=es"));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void checkForUpdates() {
        Toast.makeText(this, R.string.checking_updates, Toast.LENGTH_SHORT).show();
        loadingIndicator.setVisibility(View.VISIBLE);
        new JsonFallbackLoader(this).loadOnlineOnly(new JsonFallbackLoader.Callback() {
            @Override
            public void onSuccess(RadioResponse response) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    setupList(response.getRadios());
                    
                    String remoteVersion = response.getApp() != null ? response.getApp().getVersion() : null;
                    if (remoteVersion != null) {
                        try {
                            String localVersion = BuildConfig.VERSION_NAME;
                            // Simple string comparison or you could parse integers
                            if (!remoteVersion.equals(localVersion)) {
                                showForcedUpdateDialog();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    String version = remoteVersion != null ? remoteVersion : "Unknown";
                    Toast.makeText(MainActivity.this, getString(R.string.updated_to_version, version), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                 runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.no_updates, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showForcedUpdateDialog() {
         new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_available_title)
            .setMessage(R.string.update_available_message)
            .setCancelable(false)
            .setPositiveButton(R.string.update_now, (dialog, which) -> {
                // Open Play Store or Website
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // Fallback to browser
                     intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                     startActivity(intent);
                }
                finish(); // Close app
            })
            .show();
    }

    private void showAbout() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

        TextView textAboutMessage = view.findViewById(R.id.textAboutMessage);
        View btnEmail = view.findViewById(R.id.btnEmail);
        View btnClose = view.findViewById(R.id.btnClose);

        String verse = bibleManager.getDailyVerse();
        String message = getString(R.string.about_message) + " " + getString(R.string.contact_email) + "\n\n" +
                         getString(R.string.developer_info) + "\n\n" +
                         "\"" + verse + "\"";

        textAboutMessage.setText(message);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setView(view)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.contact_email)));
            try {
                startActivity(intent);
            } catch (Exception e) {
                // Ignore
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showWebView(String title, String url) {
        // Now using native PolicyActivity
        Intent intent = new Intent(this, PolicyActivity.class);
        startActivity(intent);
    }
    
    private void showThemeDialog() {
        showSettingsDialog();
    }

    private void showSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        
        MaterialButtonToggleGroup languageGroup = view.findViewById(R.id.languageGroup);
        android.widget.RadioGroup themeGroup = view.findViewById(R.id.themeGroup);
        View btnApply = view.findViewById(R.id.btnApply);

        // Set Current Values
        // Language
        if (languageManager.getLanguage().equals("es")) {
            languageGroup.check(R.id.langEs);
        } else {
            languageGroup.check(R.id.langEn);
        }

        // Theme
        int currentTheme = themeManager.getCurrentTheme();
        if (currentTheme == ThemeManager.THEME_LIGHT) themeGroup.check(R.id.themeLight);
        else if (currentTheme == ThemeManager.THEME_DARK) themeGroup.check(R.id.themeDark);
        else themeGroup.check(R.id.themeSystem);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setView(view)
            .create();
            
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnApply.setOnClickListener(v -> {
            boolean restartNeeded = false;

            // Language
            String newLang = languageGroup.getCheckedButtonId() == R.id.langEs ? "es" : "en";
            if (!newLang.equals(languageManager.getLanguage())) {
                languageManager.setLanguage(newLang);
                restartNeeded = true;
            }

            // Theme
            int newTheme;
            int themeId = themeGroup.getCheckedRadioButtonId();
            if (themeId == R.id.themeLight) newTheme = ThemeManager.THEME_LIGHT;
            else if (themeId == R.id.themeDark) newTheme = ThemeManager.THEME_DARK;
            else newTheme = ThemeManager.THEME_SYSTEM;

            if (newTheme != themeManager.getCurrentTheme()) {
                themeManager.setTheme(newTheme);
                // No need to set restartNeeded because setTheme already applies it, 
                // but we might want to recreate to ensure everything reloads correctly
                restartNeeded = true; 
            }

            dialog.dismiss();
            if (restartNeeded) {
                recreate();
            }
        });

        dialog.show();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void checkRateDialog() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean dontShow = prefs.getBoolean("rate_dont_show", false);
        if (dontShow) return;

        int launchCount = prefs.getInt("launch_count", 0);
        launchCount++;
        prefs.edit().putInt("launch_count", launchCount).apply();

        if (launchCount == 2) {
             showRateDialog();
        }
    }

    private void showRateDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rate_title)
            .setMessage(R.string.rate_message)
            .setPositiveButton(R.string.rate_now, (dialog, which) -> {
                 android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                 prefs.edit().putBoolean("rate_dont_show", true).apply();
                 
                 Intent intent = new Intent(Intent.ACTION_VIEW);
                 intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                 try {
                     startActivity(intent);
                 } catch (Exception e) {
                     intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                     startActivity(intent);
                 }
            })
            .setNegativeButton(R.string.rate_never, (dialog, which) -> {
                 android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                 prefs.edit().putBoolean("rate_dont_show", true).apply();
            })
            .setNeutralButton(R.string.rate_later, null)
            .show();
    }

    private void pauseRadio() {
        if (mediaController != null && mediaController.isPlaying()) {
            wasPlayingBeforeAd = true;
            mediaController.pause();
        } else {
            wasPlayingBeforeAd = false;
        }
    }
    
    private void handleMaxAction() {
        showModernMaxDialog();
    }

    private void showModernMaxDialog() {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_premium_max, null);
        
        // Setup Views
        View viewPremiumActive = dialogView.findViewById(R.id.viewPremiumActive);
        View viewFree = dialogView.findViewById(R.id.viewFree);
        
        android.widget.ProgressBar progressBarTimer = dialogView.findViewById(R.id.progressBarTimer);
        TextView textRemainingTime = dialogView.findViewById(R.id.textRemainingTime);
        TextView textAdBlockStatus = dialogView.findViewById(R.id.textAdBlockStatus);
        
        View cardOption1 = dialogView.findViewById(R.id.cardOption1);
        View cardOption3 = dialogView.findViewById(R.id.cardOption3);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        // Window background transparent to show card corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        boolean isPremium = billingManager.isPremiumPurchased();
        
        if (isPremium) {
            viewPremiumActive.setVisibility(View.VISIBLE);
            viewFree.setVisibility(View.GONE);
        } else {
            viewPremiumActive.setVisibility(View.GONE);
            viewFree.setVisibility(View.VISIBLE);
            
            // Timer Logic
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable updateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        long remaining = maxManager.getRemainingTime();
                        long totalDuration = 30 * 60 * 1000; // 30 minutes
                        if (remaining > totalDuration) totalDuration = remaining; // Adjust scale
                        
                        int progress = (int) ((remaining * 100) / totalDuration);
                        progressBarTimer.setProgress(progress);
                        
                        // Format Time HH:MM:SS
                        long seconds = remaining / 1000;
                        long minutes = seconds / 60;
                        long hours = minutes / 60;
                        minutes = minutes % 60;
                        seconds = seconds % 60;
                        textRemainingTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

                        // Ad Block Status
                        long blockTime = maxManager.getRemainingAdBlockTime();
                        if (blockTime > 0) {
                            textAdBlockStatus.setVisibility(View.VISIBLE);
                            textAdBlockStatus.setText(getString(R.string.next_ad_in, blockTime / 1000));
                            
                            // Disable cards visually (optional, logic prevents action anyway)
                            cardOption1.setAlpha(0.5f);
                        } else {
                            textAdBlockStatus.setVisibility(View.GONE);
                            cardOption1.setAlpha(1.0f);
                        }
                        
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(updateRunnable);
            
            dialog.setOnDismissListener(d -> handler.removeCallbacks(updateRunnable));

            // Button Actions
            View.OnClickListener adClickListener = v -> {
                if (maxManager.canShowAd()) {
                    // Decide reward based on logic (Both give 30m per ad)
                    final long rewardDuration = 30 * 60 * 1000;
                    
                    OnUserEarnedRewardListener rewardListener = new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            maxManager.addTime(rewardDuration);
                            maxManager.blockAdsFor(15000); // 15 seconds block
                            adView.setVisibility(View.GONE);
                            
                            Toast.makeText(MainActivity.this, R.string.max_activated, Toast.LENGTH_SHORT).show();
                            
                            // Update UI immediately
                            handler.removeCallbacks(updateRunnable);
                            handler.post(updateRunnable);
                        }
                    };

                    adsManager.showMixedRewardedAd(MainActivity.this, maxManager.isNextAdRewardedInterstitial(),
                            MainActivity.this::pauseRadio,
                            () -> {
                                if (wasPlayingBeforeAd && mediaController != null && !mediaController.isPlaying()) {
                                    mediaController.play();
                                }
                            },
                            rewardListener);

                } else {
                     long blockTime = maxManager.getRemainingAdBlockTime();
                     if (blockTime > 0) {
                         Toast.makeText(MainActivity.this, getString(R.string.wait_seconds, blockTime/1000), Toast.LENGTH_SHORT).show();
                     } else {
                         Toast.makeText(MainActivity.this, R.string.max_limit_reached, Toast.LENGTH_SHORT).show();
                     }
                }
            };

            cardOption1.setOnClickListener(adClickListener);

            cardOption3.setOnClickListener(v -> {
                billingManager.purchasePremium(this);
                dialog.dismiss();
            });
        }
        
        dialog.show();
    }

    private class RadioAdapter extends RecyclerView.Adapter<RadioAdapter.ViewHolder> {
        private final List<RadioStation> list;

        RadioAdapter(List<RadioStation> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_radio, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RadioStation station = list.get(position);
            holder.name.setText(station.getName());
            holder.desc.setText(station.getDescription());

            if (station.isPopular()) {
                holder.popular.setVisibility(View.VISIBLE);
            } else {
                holder.popular.setVisibility(View.GONE);
            }

            if (station.isPinned()) {
                holder.pinIcon.setVisibility(View.VISIBLE);
            } else {
                holder.pinIcon.setVisibility(View.GONE);
            }

            holder.itemView.setOnLongClickListener(v -> {
                boolean newState = !station.isPinned();
                station.setPinned(newState);

                android.content.SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("pinned_prefs", MODE_PRIVATE);
                java.util.Set<String> pinnedSet = new java.util.HashSet<>(prefs.getStringSet("pinned_urls", new java.util.HashSet<>()));

                if (newState) {
                    pinnedSet.add(station.getStreamUrl());
                } else {
                    pinnedSet.remove(station.getStreamUrl());
                }
                prefs.edit().putStringSet("pinned_urls", pinnedSet).apply();

                java.util.Collections.sort(list, (r1, r2) -> {
                    boolean p1 = r1.isPinned();
                    boolean p2 = r2.isPinned();
                    if (p1 && !p2) return -1;
                    if (!p1 && p2) return 1;
                    return 0;
                });
                notifyDataSetChanged();

                Toast.makeText(holder.itemView.getContext(), newState ? R.string.station_pinned : R.string.station_unpinned, Toast.LENGTH_SHORT).show();
                return true;
            });
            
            android.content.Context context = holder.itemView.getContext();
            int color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
            Drawable placeholder = ContextCompat.getDrawable(context, R.drawable.ic_music_placeholder);
            Drawable tintedPlaceholder = null;
            if (placeholder != null) {
                tintedPlaceholder = DrawableCompat.wrap(placeholder).mutate();
                DrawableCompat.setTint(tintedPlaceholder, color);
            }

            if (station.getImage() != null && !station.getImage().isEmpty()) {
                holder.image.clearColorFilter();
                Glide.with(holder.itemView)
                        .load(station.getImage())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(tintedPlaceholder)
                        .error(tintedPlaceholder)
                        .into(holder.image);
            } else {
                holder.image.setImageDrawable(tintedPlaceholder);
            }

            holder.itemView.setOnClickListener(v -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    adsManager.showInterstitial(MainActivity.this,
                        MainActivity.this::pauseRadio,
                        () -> {
                            // Save History
                            historyManager.saveLastStation(station.getName(), station.getStreamUrl(), station.getImage());

                            // Play
                            Intent intent = new Intent(MainActivity.this, RadioService.class);
                        // Actually, we use MediaController to play
                        if (mediaController != null) {
                        boolean isSameStation = false;
                        if (mediaController.getMediaItemCount() > 0) {
                            androidx.media3.common.MediaItem currentItem = mediaController.getCurrentMediaItem();
                            if (currentItem != null && currentItem.localConfiguration != null && currentItem.localConfiguration.uri != null) {
                                if (currentItem.localConfiguration.uri.toString().equals(station.getStreamUrl())) {
                                    isSameStation = true;
                                }
                            }
                        }

                        if (!isSameStation) {
                            androidx.media3.common.MediaMetadata.Builder metaBuilder = new androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(station.getName())
                                .setArtist(station.getDescription());

                            if (station.getImage() != null && !station.getImage().isEmpty()) {
                                metaBuilder.setArtworkUri(Uri.parse(station.getImage()));
                            }

                            androidx.media3.common.MediaMetadata metadata = metaBuilder.build();

                            androidx.media3.common.MediaItem mediaItem = new androidx.media3.common.MediaItem.Builder()
                                .setUri(station.getStreamUrl())
                                .setMediaMetadata(metadata)
                                .build();
                                
                            mediaController.setMediaItem(mediaItem);
                            mediaController.prepare();
                            mediaController.play();
                        } else {
                            // If same station and paused, resume
                            if (!mediaController.isPlaying()) {
                                mediaController.play();
                            }
                        }
                        
                        // Open player
                        Intent playerIntent = new Intent(MainActivity.this, PlayerActivity.class);
                        startActivity(playerIntent);
                    }
                });
              });
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, desc;
            ImageView image, popular, pinIcon;
            View cardContainer;

            ViewHolder(View itemView) {
                super(itemView);
                cardContainer = itemView.findViewById(R.id.cardContainer);
                name = itemView.findViewById(R.id.stationName);
                desc = itemView.findViewById(R.id.stationDesc);
                image = itemView.findViewById(R.id.stationImage);
                popular = itemView.findViewById(R.id.popularIcon);
                pinIcon = itemView.findViewById(R.id.pinIcon);
            }
        }
    }
}
