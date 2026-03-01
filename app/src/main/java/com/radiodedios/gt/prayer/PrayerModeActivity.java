package com.radiodedios.gt.prayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import java.util.List;
import java.util.Locale;

public class PrayerModeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

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
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private Prayer currentPrayer;

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
        tts = new TextToSpeech(this, this);

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
            if (tts != null && tts.isSpeaking()) {
                tts.stop();
            }
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
        if (isTtsReady) {
            String textToSpeak = prayer.getText() + ". " + prayer.getVerse();
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "PrayerTTS");
        } else {
            Toast.makeText(this, R.string.tts_not_ready, Toast.LENGTH_SHORT).show();
        }
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
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale spanishLocale = new Locale("es", "ES");
            int result = tts.setLanguage(spanishLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try default locale
                result = tts.setLanguage(Locale.getDefault());
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true;
                }
            } else {
                isTtsReady = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
