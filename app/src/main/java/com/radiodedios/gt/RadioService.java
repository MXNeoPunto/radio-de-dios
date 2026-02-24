package com.radiodedios.gt;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.metadata.icy.IcyInfo;
import androidx.media3.extractor.metadata.id3.TextInformationFrame;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.radiodedios.gt.R;
import com.radiodedios.gt.data.JsonFallbackLoader;
import com.radiodedios.gt.model.RadioResponse;
import com.radiodedios.gt.model.RadioStation;
import com.radiodedios.gt.manager.StatsManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@OptIn(markerClass = UnstableApi.class)
public class RadioService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;
    
    // Retry logic
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private RadioStation currentStation;

    private Handler statsHandler = new Handler(Looper.getMainLooper());
    private Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            StatsManager.getInstance(RadioService.this).addPlayTime(60000);
            statsHandler.postDelayed(this, 60000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Setup HTTP DataSource with Icy-MetaData header
        Map<String, String> defaultRequestProperties = new HashMap<>();
        defaultRequestProperties.put("Icy-MetaData", "1");
        
        String userAgent = "RadioDeDios/1.0 (Linux;Android " + android.os.Build.VERSION.RELEASE + ") ExoPlayerLib/2.18.7";

        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(defaultRequestProperties)
                .setAllowCrossProtocolRedirects(true);
        
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(this)
                .setDataSourceFactory(httpDataSourceFactory);
        
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();
                
        player.addListener(new PlayerListener());
        
        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(new CustomMediaSessionCallback())
                .build();
    }

    private class CustomMediaSessionCallback implements MediaSession.Callback {
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(MediaSession session,
                MediaSession.ControllerInfo controller,
                SessionCommand customCommand, Bundle args) {

            if ("ACTION_DJ_FADE_OUT".equals(customCommand.customAction)) {
                startFadeOut();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            } else if ("ACTION_DJ_FADE_IN".equals(customCommand.customAction)) {
                startFadeIn();
                return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
            }
            return MediaSession.Callback.super.onCustomCommand(session, controller, customCommand, args);
        }
    }

    private void startFadeOut() {
        final int FADE_DURATION = 2000;
        final int STEPS = 20;
        final int DELAY = FADE_DURATION / STEPS;
        final float MIN_VOLUME = 0.1f;

        if (player == null) return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (player == null) return;
                float currentVol = player.getVolume();

                if (step < STEPS && currentVol > MIN_VOLUME) {
                    float newVol = currentVol - ((1.0f - MIN_VOLUME) / STEPS);
                    if (newVol < MIN_VOLUME) newVol = MIN_VOLUME;
                    player.setVolume(newVol);
                    step++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, DELAY);
                } else {
                    player.setVolume(MIN_VOLUME);
                }
            }
        });
    }

    private void startFadeIn() {
        final int FADE_DURATION = 2000;
        final int STEPS = 20;
        final int DELAY = FADE_DURATION / STEPS;
        final float TARGET_VOLUME = 1.0f;

        if (player == null) return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (player == null) return;
                float currentVol = player.getVolume();

                if (step < STEPS && currentVol < TARGET_VOLUME) {
                    float newVol = currentVol + ((TARGET_VOLUME - 0.1f) / STEPS);
                    if (newVol > TARGET_VOLUME) newVol = TARGET_VOLUME;
                    player.setVolume(newVol);
                    step++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, DELAY);
                } else {
                    player.setVolume(TARGET_VOLUME);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (statsHandler != null && statsRunnable != null) {
            statsHandler.removeCallbacks(statsRunnable);
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.radiodedios.gt.ACTION_STOP_PLAYBACK".equals(intent.getAction())) {
            if (player != null) {
                player.pause();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class PlayerListener implements Player.Listener {
        @Override
        public void onMetadata(Metadata metadata) {
            for (int i = 0; i < metadata.length(); i++) {
                Metadata.Entry entry = metadata.get(i);
                if (entry instanceof IcyInfo) {
                    IcyInfo icyInfo = (IcyInfo) entry;
                    if (icyInfo.title != null) {
                         updateMetadata(icyInfo.title);
                    }
                } else if (entry instanceof TextInformationFrame) {
                    TextInformationFrame id3 = (TextInformationFrame) entry;
                    if ("TIT2".equals(id3.id) || "TT2".equals(id3.id)) {
                        if (!id3.values.isEmpty()) {
                            updateMetadata(id3.values.get(0));
                        }
                    }
                }
            }
        }
        
        @Override
        public void onPlayerError(PlaybackException error) {
            if (retryCount < MAX_RETRIES) {
                retryCount++;
                String msg = getString(R.string.retry_connection, retryCount);
                Toast.makeText(RadioService.this, msg, Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                     if (player != null) {
                         player.prepare();
                         player.play();
                     }
                }, 2000);
            } else {
                Toast.makeText(RadioService.this, R.string.switching_station, Toast.LENGTH_SHORT).show();
                playRandomStation();
            }
        }
        
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (isPlaying) {
                retryCount = 0;
                statsHandler.removeCallbacks(statsRunnable);
                statsHandler.postDelayed(statsRunnable, 60000);
            } else {
                statsHandler.removeCallbacks(statsRunnable);
            }
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            if (mediaItem != null && mediaItem.mediaMetadata != null && mediaItem.mediaMetadata.title != null) {
                StatsManager.getInstance(RadioService.this).incrementStationPlay(mediaItem.mediaMetadata.title.toString());
            }
        }
    }
    
    private void updateMetadata(String title) {
        if (currentStation == null) return;
        if (player.getCurrentMediaItem() == null) return;

        MediaMetadata currentMeta = player.getMediaMetadata();
        MediaMetadata.Builder builder = currentMeta.buildUpon();
        
        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
            // Set station name as artist so it's visible
            builder.setArtist(currentStation.getName());
        } else {
            builder.setTitle(currentStation.getName());
            builder.setArtist(currentStation.getDescription());
        }

        // Ensure Artwork is there
        if (currentMeta.artworkUri == null && currentStation.getImage() != null && !currentStation.getImage().isEmpty()) {
             builder.setArtworkUri(Uri.parse(currentStation.getImage()));
        }
        
        // Update metadata by replacing the current item (seamlessly)
        MediaItem currentItem = player.getCurrentMediaItem();
        MediaItem newItem = currentItem.buildUpon()
                .setMediaMetadata(builder.build())
                .build();
        
        player.replaceMediaItem(player.getCurrentMediaItemIndex(), newItem);
    }

    private void playRandomStation() {
        new JsonFallbackLoader(this).loadData(new JsonFallbackLoader.Callback() {
            @Override
            public void onSuccess(RadioResponse response) {
                if (response.getRadios() != null && !response.getRadios().isEmpty()) {
                    List<RadioStation> list = response.getRadios();
                    RadioStation randomStation = list.get(new Random().nextInt(list.size()));
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                         playStationInternal(randomStation);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });
    }

    // Helper method to start playback (internal use)
    private void playStationInternal(RadioStation station) {
        currentStation = station;
        retryCount = 0;
        
        MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                .setTitle(station.getName())
                .setArtist(station.getDescription());

        if (station.getImage() != null && !station.getImage().isEmpty()) {
             metaBuilder.setArtworkUri(Uri.parse(station.getImage()));
        }
        
        MediaMetadata metadata = metaBuilder.build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(station.getStreamUrl())
                .setMediaMetadata(metadata)
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
}
