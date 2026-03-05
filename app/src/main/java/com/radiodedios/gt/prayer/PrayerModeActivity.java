package com.radiodedios.gt.prayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.media.MediaPlayer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.radiodedios.gt.R;
import com.radiodedios.gt.manager.LanguageManager;

import com.radiodedios.gt.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrayerModeActivity extends AppCompatActivity {

    private EditText etName;
    private AutoCompleteTextView spinnerCategory;
    private EditText etDescription;
    private Button btnGenerate;
    private ImageButton btnHistory;
    private ImageButton btnBack;

    private LinearLayout layoutForm;
    private LinearLayout layoutResult;

    private TextView tvGeneratedPrayer;
    private TextView tvGeneratedVerse;
    private Button btnAmen;
    private Button btnBlessings;
    private ImageButton btnShareResult;
    private ImageButton btnListenResult;
    private Button btnNewPrayer;

    private PrayerGenerator generator;
    private Prayer currentPrayer;

    private MediaPlayer mediaPlayer;
    private ExecutorService executorService;
    private boolean isPlaying = false;
    private File tempAudioFile;

    private static final String ELEVENLABS_API_KEY = BuildConfig.ELEVENLABS_API_KEY;
    private static final String ELEVENLABS_VOICE_ID = "2zRM7PkgwBPiau2jvVXc";

    @Override
    protected void attachBaseContext(Context newBase) {
        LanguageManager langMgr = new LanguageManager(newBase);
        super.attachBaseContext(langMgr.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_mode);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        generator = new PrayerGenerator(this);
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etDescription = findViewById(R.id.etDescription);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnHistory = findViewById(R.id.btnHistory);
        btnBack = findViewById(R.id.btnBack);

        layoutForm = findViewById(R.id.layoutForm);
        layoutResult = findViewById(R.id.layoutResult);

        tvGeneratedPrayer = findViewById(R.id.tvGeneratedPrayer);
        tvGeneratedVerse = findViewById(R.id.tvGeneratedVerse);
        btnAmen = findViewById(R.id.btnAmen);
        btnBlessings = findViewById(R.id.btnBlessings);
        btnShareResult = findViewById(R.id.btnShareResult);
        btnListenResult = findViewById(R.id.btnListenResult);
        btnNewPrayer = findViewById(R.id.btnNewPrayer);
    }

    private void setupSpinner() {
        String[] categories = getResources().getStringArray(R.array.prayer_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnHistory.setOnClickListener(v -> showHistoryDialog());

        btnGenerate.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String category = spinnerCategory.getText().toString();
            String description = etDescription.getText().toString();

            if (category.isEmpty()) {
                Toast.makeText(this, R.string.prayer_select_category_error, Toast.LENGTH_SHORT).show();
                return;
            }

            currentPrayer = generator.generatePrayer(name, category, description);
            showResult(currentPrayer);
        });

        btnNewPrayer.setOnClickListener(v -> {
            layoutResult.setVisibility(View.GONE);
            layoutForm.setVisibility(View.VISIBLE);
            etName.setText("");
            etDescription.setText("");
        });

        btnAmen.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            int count = prefs.getInt("amen_count", 0) + 1;
            prefs.edit().putInt("amen_count", count).apply();
            Toast.makeText(this, getString(R.string.amen_counted, count), Toast.LENGTH_SHORT).show();
        });

        btnBlessings.setOnClickListener(v -> {
            Toast.makeText(this, R.string.blessings_message, Toast.LENGTH_LONG).show();
        });

        btnShareResult.setOnClickListener(v -> {
            if (currentPrayer != null) {
                sharePrayer(currentPrayer);
            }
        });

        btnListenResult.setOnClickListener(v -> {
            if (currentPrayer != null) {
                listenPrayer(currentPrayer);
            }
        });
    }

    private void showResult(Prayer prayer) {
        layoutForm.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);
        tvGeneratedPrayer.setText(prayer.getText());
        tvGeneratedVerse.setText(prayer.getVerse());
    }

    private void sharePrayer(Prayer prayer) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = prayer.getText() + "\n\n" + prayer.getVerse() + "\n\n" + getString(R.string.share_message);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
    }

    private void listenPrayer(Prayer prayer) {
        if (isPlaying) {
            stopAudio();
            return;
        }

        btnListenResult.setEnabled(false);
        Toast.makeText(this, R.string.generating_audio, Toast.LENGTH_SHORT).show();

        String separator = getString(R.string.prayer_verse_separator);
        String verse = prayer.getVerse();
        verse = verse.replace(":", " " + separator + " ");
        String textToSpeak = prayer.getText() + ". " + verse;

        fetchElevenLabsAudio(textToSpeak);
    }

    private void fetchElevenLabsAudio(String text) {
        executorService.execute(() -> {
            try {
                URL url = new URL("https://api.elevenlabs.io/v1/text-to-speech/" + ELEVENLABS_VOICE_ID);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("xi-api-key", ELEVENLABS_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "audio/mpeg");
                conn.setDoOutput(true);

                JSONObject payload = new JSONObject();
                payload.put("text", text);
                payload.put("model_id", "eleven_multilingual_v2");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    tempAudioFile = File.createTempFile("prayer_tts", ".mp3", getCacheDir());
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(tempAudioFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    runOnUiThread(() -> playAudio(tempAudioFile));
                } else {
                    runOnUiThread(() -> {
                        btnListenResult.setEnabled(true);
                        Toast.makeText(PrayerModeActivity.this, "Error: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnListenResult.setEnabled(true);
                    Toast.makeText(PrayerModeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                });
            } catch (JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnListenResult.setEnabled(true);
                    Toast.makeText(PrayerModeActivity.this, "Error parsing API request", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void playAudio(File audioFile) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(mp -> stopAudio());
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            btnListenResult.setEnabled(true);
            btnListenResult.setImageResource(R.drawable.ic_pause);
        } catch (IOException e) {
            e.printStackTrace();
            btnListenResult.setEnabled(true);
            Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        isPlaying = false;
        btnListenResult.setImageResource(R.drawable.ic_play);
    }

    private void showHistoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_prayer_history, null);
        RecyclerView recyclerView = view.findViewById(R.id.rvPrayerHistory);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyHistory);

        List<Prayer> history = generator.getHistory();
        if (history.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            PrayerHistoryAdapter adapter = new PrayerHistoryAdapter(history, new PrayerHistoryAdapter.OnPrayerClickListener() {
                @Override
                public void onListenClicked(Prayer prayer) {
                    listenPrayer(prayer);
                }

                @Override
                public void onShareClicked(Prayer prayer) {
                    sharePrayer(prayer);
                }
            });
            recyclerView.setAdapter(adapter);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton(R.string.close, (d, w) -> d.dismiss())
                .setNeutralButton(R.string.clear_all, (d, w) -> {
                    generator.clearHistory();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (tempAudioFile != null && tempAudioFile.exists()) {
            tempAudioFile.delete();
        }
        super.onDestroy();
    }
}
