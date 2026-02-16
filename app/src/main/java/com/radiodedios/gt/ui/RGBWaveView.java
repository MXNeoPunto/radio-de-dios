package com.radiodedios.gt.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class RGBWaveView extends View {

    private Paint paint;
    private Path path;
    private float phase = 0;
    private boolean isPlaying = false;
    private float amplitude = 1.0f;

    public RGBWaveView(Context context) {
        super(context);
        init();
    }

    public RGBWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f); // Thicker line
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path = new Path();
    }

    public void startAnimation() {
        isPlaying = true;
        invalidate();
    }

    public void stopAnimation() {
        isPlaying = false;
        phase = 0;
        invalidate();
    }
    
    // Simulate audio data
    public void setAmplitude(float amp) {
        this.amplitude = 0.5f + (amp * 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isPlaying) {
             // Draw a flat line or nothing
             return;
        }

        int width = getWidth();
        int height = getHeight();
        float centerY = height / 2f;

        // Dynamic Gradient
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFF0000};
        float[] positions = {0f, 0.33f, 0.66f, 1f};
        Shader shader = new LinearGradient(0, 0, width, 0, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(shader);

        path.reset();
        path.moveTo(0, centerY);

        // Sine Wave simulation
        // We use multiple sine waves combined to look more "organic" like an audio wave
        for (float x = 0; x < width; x += 5) {
            float normalizedX = x / width;
            
            // Envelope (taper ends)
            float envelope = (float) Math.sin(normalizedX * Math.PI); 
            
            float y = centerY + 
                (float) Math.sin(x * 0.05f + phase) * 50 * envelope * amplitude +
                (float) Math.sin(x * 0.1f + phase * 2.0f) * 20 * envelope;

            path.lineTo(x, y);
        }
        
        canvas.drawPath(path, paint);

        phase += 0.2f;
        
        // Randomly fluctuate amplitude slightly to look alive
        amplitude = 0.8f + (float)Math.sin(phase * 0.5f) * 0.3f;

        postInvalidateOnAnimation();
    }
}
