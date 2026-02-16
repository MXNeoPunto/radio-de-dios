package com.radiodedios.gt.manager;

import android.os.CountDownTimer;

public class SleepTimerManager {
    private CountDownTimer countDownTimer;
    private long remainingTimeMillis = 0;
    private TimerListener listener;

    public interface TimerListener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    public void startTimer(int minutes, TimerListener listener) {
        cancelTimer();
        this.listener = listener;
        remainingTimeMillis = minutes * 60 * 1000L;

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
