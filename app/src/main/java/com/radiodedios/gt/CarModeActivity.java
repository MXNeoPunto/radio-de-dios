package com.radiodedios.gt;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.radiodedios.gt.manager.LanguageManager;
import com.radiodedios.gt.manager.ThemeManager;
import java.util.concurrent.ExecutionException;

public class CarModeActivity extends AppCompatActivity {
    
    private MediaController mediaController;
    private Button btnPlayPause;

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
        btnPlayPause = findViewById(R.id.btnPlayPause); // Need to add ID to layout
        
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
        
        setupMediaController();
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
}
