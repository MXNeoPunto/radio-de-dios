package com.radiodedios.gt.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleView extends View {

    private final List<Particle> particles = new ArrayList<>();
    private Paint paint;
    private Random random = new Random();
    private boolean isEnabled = true;

    private class Particle {
        float x, y;
        float speedY;
        float alpha;
        float size;
    }

    public ParticleView(Context context) {
        super(context);
        init();
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        updateColor();
        paint.setStyle(Paint.Style.FILL);
    }

    private void updateColor() {
        // Detect theme
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
             paint.setColor(0xFFFFFFFF); // White for Dark Mode
        } else {
             paint.setColor(0xFF4F5CD1); // Primary Color (Indigo) for Light Mode
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled) {
            particles.clear();
            invalidate();
        } else {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        updateColor(); // Ensure color updates if config changes
        if (!isEnabled) return;

        int width = getWidth();
        int height = getHeight();

        // Spawn particles
        if (particles.size() < 30) {
            Particle p = new Particle();
            p.x = random.nextFloat() * width;
            p.y = height + 10;
            p.speedY = 1.0f + random.nextFloat() * 2.0f; // Slow rise
            p.alpha = 255;
            p.size = 2.0f + random.nextFloat() * 4.0f;
            particles.add(p);
        }

        // Update and Draw
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.y -= p.speedY;
            p.alpha -= 0.5f;

            if (p.y < 0 || p.alpha <= 0) {
                particles.remove(i);
                continue;
            }

            paint.setAlpha((int) p.alpha);
            canvas.drawCircle(p.x, p.y, p.size, paint);
        }

        postInvalidateOnAnimation();
    }
}
