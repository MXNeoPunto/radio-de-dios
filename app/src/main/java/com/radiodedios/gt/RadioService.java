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
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import com.radiodedios.gt.R;
import com.radiodedios.gt.data.JsonFallbackLoader;
import com.radiodedios.gt.model.RadioResponse;
import com.radiodedios.gt.model.RadioStation;
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
        
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Override
    public void onDestroy() {
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
