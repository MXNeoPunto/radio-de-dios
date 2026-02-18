package com.radiodedios.gt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

public class BatteryOptimizationHelper {
    public static void checkBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                android.view.View view = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_battery_tutorial, null);

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setView(view)
                        .setCancelable(true)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                }

                view.findViewById(R.id.btnGoToSettings).setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    context.startActivity(intent);
                    dialog.dismiss();
                });

                view.findViewById(R.id.btnLater).setOnClickListener(v -> dialog.dismiss());

                dialog.show();
            }
        }
    }
}
