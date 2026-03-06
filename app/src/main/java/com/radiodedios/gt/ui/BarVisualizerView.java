package com.radiodedios.gt.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;
import android.view.Choreographer;

import androidx.annotation.Nullable;
import com.google.android.material.color.MaterialColors;
import android.graphics.Color;

public class BarVisualizerView extends View implements Choreographer.FrameCallback {

    private Paint paint;
    private Visualizer visualizer;
    private float[] amplitudes;
    private float[] targetAmplitudes;

    // Number of bars
    private int barCount = 32;
    private float barWidth;
    private float gap;
    private float cornerRadius;

    private int colorPrimary;

    private boolean isAnimating = false;

    // FPS configuration
    private int fpsLimit = 60;
    private long frameIntervalNs = 1000000000L / 60;
    private long lastFrameTimeNs = 0;

    private RectF rectF = new RectF();

    public BarVisualizerView(Context context) {
        super(context);
        init(context);
    }

    public BarVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Resolve primary color directly via theme to avoid missing attribute resource IDs
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        colorPrimary = typedValue.data;

        paint.setColor(colorPrimary);

        amplitudes = new float[barCount];
        targetAmplitudes = new float[barCount];

        loadFpsSettings(context);
    }

    public void loadFpsSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        fpsLimit = prefs.getInt("visualizer_fps", 60); // Default to 60
        frameIntervalNs = 1000000000L / fpsLimit;
    }

    public void linkToAudioSession(int audioSessionId) {
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }

        if (audioSessionId != -1) {
            try {
                visualizer = new Visualizer(audioSessionId);
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                        // Not using waveform
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                        processFft(fft);
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true);
                visualizer.setEnabled(true);
            } catch (SecurityException e) {
                // User may not have granted RECORD_AUDIO permission
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processFft(byte[] fft) {
        if (fft == null || fft.length < 2) return;

        // Group FFT data into barCount bands
        int usableFftLength = Math.min(fft.length / 2, 80); // Focus on lower/mid frequencies
        if (usableFftLength < barCount) return;

        int bandSize = usableFftLength / barCount;

        for (int i = 0; i < barCount; i++) {
            float sum = 0;
            for (int j = 0; j < bandSize; j++) {
                int index = (i * bandSize + j) * 2;
                if (index + 1 < fft.length) {
                    byte real = fft[index];
                    byte imag = fft[index + 1];
                    sum += (float) Math.hypot(real, imag);
                }
            }
            // Normalize and set target
            float mag = (sum / bandSize) / 256f;
            targetAmplitudes[i] = mag;
        }
    }

    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    public void stopAnimation() {
        isAnimating = false;
        Choreographer.getInstance().removeFrameCallback(this);

        // Reset amplitudes gracefully
        for(int i=0; i<barCount; i++) {
            targetAmplitudes[i] = 0f;
            amplitudes[i] = 0f;
        }
        invalidate();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isAnimating) return;

        if (frameTimeNanos - lastFrameTimeNs >= frameIntervalNs) {
            lastFrameTimeNs = frameTimeNanos;

            // Smoothly interpolate amplitudes
            boolean needsDraw = false;
            for(int i=0; i<barCount; i++) {
                if (Math.abs(amplitudes[i] - targetAmplitudes[i]) > 0.01f) {
                    // Quick rise, slow fall
                    if (targetAmplitudes[i] > amplitudes[i]) {
                        amplitudes[i] = amplitudes[i] * 0.4f + targetAmplitudes[i] * 0.6f;
                    } else {
                        amplitudes[i] = amplitudes[i] * 0.85f + targetAmplitudes[i] * 0.15f;
                    }
                    needsDraw = true;
                } else {
                    amplitudes[i] = targetAmplitudes[i];
                }
            }

            if (needsDraw) {
                invalidate();
            }
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        gap = w * 0.015f;
        barWidth = (w - (gap * (barCount - 1))) / barCount;
        cornerRadius = barWidth / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = getHeight();
        float width = getWidth();
        float centerY = height / 2f;

        // Draw bars centered vertically
        for (int i = 0; i < barCount; i++) {
            float x = i * (barWidth + gap);

            // Minimum height for aesthetics
            float minHeight = 10f;
            float barHeight = Math.max(minHeight, amplitudes[i] * height * 0.8f);

            float top = centerY - (barHeight / 2f);
            float bottom = centerY + (barHeight / 2f);

            rectF.set(x, top, x + barWidth, bottom);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
    }
}
