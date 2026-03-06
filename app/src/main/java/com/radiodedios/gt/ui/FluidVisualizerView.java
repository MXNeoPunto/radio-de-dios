package com.radiodedios.gt.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;
import android.view.Choreographer;

import androidx.annotation.Nullable;
import com.google.android.material.color.MaterialColors;
import android.graphics.Color;

import java.util.Random;

public class FluidVisualizerView extends View implements Choreographer.FrameCallback {

    private Paint paint;
    private Path path;
    private Visualizer visualizer;
    private byte[] rawAudioBytes;
    private float amplitude = 0f;
    private float agitaion = 0f;
    private float baseRadius;
    private float time = 0f;

    private int colorPrimary;
    private int colorSecondaryContainer;
    private int colorTertiary;
    private int colorSurface;

    private boolean isAnimating = false;
    private Random random = new Random();

    // Pre-allocated arrays for onDraw to avoid GC churn
    private int[] gradientColors;
    private float[] gradientPositions = new float[]{0f, 0.4f, 0.8f, 1f};
    private float[] pointsX;
    private float[] pointsY;
    private int points = 12;
    private float angleStep = (float) (Math.PI * 2 / points);
    private android.graphics.Matrix gradientMatrix;

    // FPS configuration
    private int fpsLimit = 60;
    private long frameIntervalNs = 1000000000L / 60;
    private long lastFrameTimeNs = 0;

    public FluidVisualizerView(Context context) {
        super(context);
        init(context);
    }

    public FluidVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();

        // Material 3 Dynamic Colors
        colorPrimary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, Color.MAGENTA);
        colorSecondaryContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondaryContainer, Color.BLUE);
        colorTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary, Color.CYAN);
        colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK);

        gradientColors = new int[]{colorPrimary, colorSecondaryContainer, colorTertiary, colorSurface};
        pointsX = new float[points];
        pointsY = new float[points];
        gradientMatrix = new android.graphics.Matrix();

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
        // Calculate bass amplitude (low frequencies)
        float bassSum = 0;
        int bassCount = Math.min(fft.length, 10);
        for (int i = 0; i < bassCount; i++) {
            byte real = fft[i * 2];
            byte imag = fft[i * 2 + 1];
            float mag = (float) Math.hypot(real, imag);
            bassSum += mag;
        }
        float currentBass = (bassCount > 0) ? (bassSum / bassCount) / 256f : 0;

        // Smoothly update amplitude
        amplitude = amplitude * 0.7f + currentBass * 0.3f;

        // Calculate treble agitation (high frequencies)
        float trebleSum = 0;
        int trebleStart = Math.min(fft.length / 2, 20);
        int trebleCount = Math.min(fft.length, 60) - trebleStart;
        if(trebleCount > 0) {
            for (int i = trebleStart; i < trebleStart + trebleCount; i++) {
                byte real = fft[i * 2];
                byte imag = fft[i * 2 + 1];
                float mag = (float) Math.hypot(real, imag);
                trebleSum += mag;
            }
            float currentTreble = (trebleSum / trebleCount) / 256f;
            agitaion = agitaion * 0.7f + currentTreble * 0.3f;
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
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isAnimating) return;

        if (frameTimeNanos - lastFrameTimeNs >= frameIntervalNs) {
            lastFrameTimeNs = frameTimeNanos;
            time += 0.08f + (agitaion * 0.15f);
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        baseRadius = Math.min(w, h) / 3f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // Dynamic radius based on bass amplitude
        float currentRadius = baseRadius + (baseRadius * amplitude * 1.8f);

        // Update gradient based on size
        RadialGradient gradient = new RadialGradient(cx, cy, currentRadius * 1.5f,
                gradientColors,
                gradientPositions,
                Shader.TileMode.CLAMP);

        gradientMatrix.setRotate(time * 50f, cx, cy);
        gradient.setLocalMatrix(gradientMatrix);
        paint.setShader(gradient);

        // Fluid Blob Path
        path.reset();

        for (int i = 0; i < points; i++) {
            float angle = i * angleStep;
            float deform = (float) (Math.sin(angle * 3 + time) + Math.cos(angle * 4 - time * 0.8f)) * (agitaion * 80f + 15f + amplitude * 30f);
            float r = currentRadius + deform;

            pointsX[i] = cx + (float) Math.cos(angle) * r;
            pointsY[i] = cy + (float) Math.sin(angle) * r;
        }

        path.moveTo(
            (pointsX[0] + pointsX[points - 1]) / 2,
            (pointsY[0] + pointsY[points - 1]) / 2
        );

        for (int i = 0; i < points; i++) {
            float nextX = pointsX[(i + 1) % points];
            float nextY = pointsY[(i + 1) % points];
            float currX = pointsX[i];
            float currY = pointsY[i];

            float midX = (currX + nextX) / 2;
            float midY = (currY + nextY) / 2;

            path.quadTo(currX, currY, midX, midY);
        }

        path.close();

        canvas.drawPath(path, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
