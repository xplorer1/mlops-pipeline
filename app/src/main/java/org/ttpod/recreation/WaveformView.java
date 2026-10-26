package org.ttpod.recreation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import org.ttpod.recreation.playback.WaveformBus;

/** Java keeps this custom XML view available to view binding during clean Javac builds. */
public final class WaveformView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WaveformView(Context context) { this(context, null); }
    public WaveformView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setColor(0xff31adff);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 2f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);
        float[] values = WaveformBus.INSTANCE.snapshot();
        float middle = getHeight() / 2f;
        for (int i = 1; i < values.length; i++) {
            canvas.drawLine((i - 1) * getWidth() / (float)(values.length - 1), middle - values[i - 1] * middle * .8f,
                i * getWidth() / (float)(values.length - 1), middle - values[i] * middle * .8f, paint);
        }
        if (isShown()) postInvalidateDelayed(33);
    }
}
