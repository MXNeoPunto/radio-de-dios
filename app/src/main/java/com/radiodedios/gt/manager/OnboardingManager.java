package com.radiodedios.gt.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ViewFlipper;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.radiodedios.gt.R;

public class OnboardingManager {

    private final Activity activity;
    private final SharedPreferences prefs;
    private static final String PREF_ONBOARDING_COMPLETE = "onboarding_complete_v1";

    public OnboardingManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public void showOnboardingIfNeeded() {
        if (prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)) {
            return;
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_onboarding, null);
        ViewFlipper flipper = view.findViewById(R.id.onboardingFlipper);

        // Setup animations
        flipper.setInAnimation(activity, R.anim.slide_in_right);
        flipper.setOutAnimation(activity, R.anim.slide_out_left);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        }

        // --- Step 1: Battery ---
        View btnAllowBattery = view.findViewById(R.id.btnAllowBattery);
        View btnSkipBattery = view.findViewById(R.id.btnSkipBattery);

        btnAllowBattery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                activity.startActivity(intent);
            }
            flipper.showNext();
        });

        btnSkipBattery.setOnClickListener(v -> flipper.showNext());

        // --- Step 2: Notifications ---
        View btnAllowNotif = view.findViewById(R.id.btnAllowNotif);
        View btnDenyNotif = view.findViewById(R.id.btnDenyNotif);

        btnAllowNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
            flipper.showNext();
        });

        btnDenyNotif.setOnClickListener(v -> flipper.showNext());

        // --- Step 3: Ads Info ---
        View btnFinishOnboarding = view.findViewById(R.id.btnFinishOnboarding);

        btnFinishOnboarding.setOnClickListener(v -> {
            prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, true).apply();
            dialog.dismiss();
        });

        dialog.show();
    }
}
