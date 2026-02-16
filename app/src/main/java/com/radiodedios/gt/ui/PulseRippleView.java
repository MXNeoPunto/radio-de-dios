package com.radiodedios.gt.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PulseRippleView extends View {

    private Paint paint;
    private List<Ripple> ripples = new ArrayList<>();
    private boolean isPulsing = false;
    private long lastPulseTime = 0;
    private int pulseInterval = 800; // ms between beats

    public PulseRippleView(Context context) {
        super(context);
        init();
    }

    public PulseRippleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        
        // Use theme color (colorOnSurface)
        int color = 0xFFFFFFFF; // Default white
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)) {
             color = typedValue.data;
        }
        paint.setColor(color);
    }

    public void startRipple() {
        if (!isPulsing) {
            isPulsing = true;
            postInvalidateOnAnimation();
        }
    }

    public void stopRipple() {
        isPulsing = false;
        ripples.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isPulsing) {
            long now = System.currentTimeMillis();
            if (now - lastPulseTime > pulseInterval) {
                ripples.add(new Ripple(Math.min(getWidth(), getHeight()) / 2f));
                lastPulseTime = now;
            }
        }

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxRadius = Math.max(getWidth(), getHeight()) / 1.5f;

        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            ripple.radius += 5f; // Expansion speed
            
            float alpha = 255f * (1f - (ripple.radius / maxRadius));
            if (alpha <= 0) {
                iterator.remove();
            } else {
                paint.setAlpha((int) alpha);
                canvas.drawCircle(cx, cy, ripple.radius, paint);
            }
        }

        if (isPulsing || !ripples.isEmpty()) {
            postInvalidateOnAnimation();
        }
    }

    private static class Ripple {
        float radius;
        Ripple(float startRadius) {
            this.radius = startRadius;
        }
    }
}
