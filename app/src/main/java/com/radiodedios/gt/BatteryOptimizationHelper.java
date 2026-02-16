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
                new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.battery_saver_title))
                    .setMessage(context.getString(R.string.battery_saver_message))
                    .setPositiveButton(context.getString(R.string.go_to_settings), (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        context.startActivity(intent);
                    })
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show();
            }
        }
    }
}
