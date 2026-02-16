package com.radiodedios.gt.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Random;

public class AudioVisualizerView extends View {

    private byte[] bytes;
    private float[] points;
    private Paint paint = new Paint();
    private int strokeWidth = 10;
    private Random random = new Random();

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bytes = null;
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#9C27B0")); // Purple
    }

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    // Simulation for now as we don't have real FFT hooks from ExoPlayer easily without a lot of boilerplate
    // In a real app we would use a Visualizer session id or an AudioProcessor
    public void simulateAudio() {
        // Highs: Lines animation
        if (bytes == null) bytes = new byte[40];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (Math.abs(random.nextInt()) % 100);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null) return;

        if (points == null || points.length < bytes.length * 4) {
            points = new float[bytes.length * 4];
        }

        int width = getWidth();
        int height = getHeight();
        int barWidth = width / bytes.length;

        for (int i = 0; i < bytes.length; i++) {
            int x = i * barWidth + (barWidth / 2);
            int magnitude = (int) (bytes[i] + 128); // Convert signed byte to unsigned
            int barHeight = (int) ((magnitude / 256.0) * height);
            
            // Draw vertical bar centered vertically or from bottom
            // From bottom looks better for bars
            float top = height - barHeight;
            float bottom = height;
            
            // Or centered like waveform
            // float top = (height / 2) - (barHeight / 2);
            // float bottom = (height / 2) + (barHeight / 2);
            
            paint.setStrokeWidth(barWidth - 2); // Leave some gap
            canvas.drawLine(x, top, x, bottom, paint);
        }
    }
}
