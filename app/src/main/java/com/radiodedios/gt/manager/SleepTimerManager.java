package com.radiodedios.gt.manager;

import android.os.CountDownTimer;

import android.content.Context;
import android.content.Intent;

public class SleepTimerManager {
    private static SleepTimerManager instance;
    private CountDownTimer countDownTimer;
    private long remainingTimeMillis = 0;
    private TimerListener listener;

    private SleepTimerManager() {}

    public static synchronized SleepTimerManager getInstance() {
        if (instance == null) {
            instance = new SleepTimerManager();
        }
        return instance;
    }

    public interface TimerListener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    public void startTimer(Context context, int minutes, TimerListener listener) {
        cancelTimer();
        this.listener = listener;
        remainingTimeMillis = minutes * 60 * 1000L;
        final Context appContext = context.getApplicationContext();

        countDownTimer = new CountDownTimer(remainingTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                if (SleepTimerManager.this.listener != null) {
                    SleepTimerManager.this.listener.onTick(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                remainingTimeMillis = 0;
                if (SleepTimerManager.this.listener != null) {
                    SleepTimerManager.this.listener.onFinish();
                }

                // Stop Service
                Intent intent = new Intent(appContext, com.radiodedios.gt.RadioService.class);
                intent.setAction("com.radiodedios.gt.ACTION_STOP_PLAYBACK");
                try {
                    appContext.startService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        remainingTimeMillis = 0;
    }

    public boolean isTimerRunning() {
        return countDownTimer != null;
    }
    
    public long getRemainingTimeMillis() {
        return remainingTimeMillis;
    }
}
