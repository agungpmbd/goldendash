package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Speedometer: cincin 270° (celah di bawah) yang terisi biru sampai 140 km/j, angka besar di tengah. */
final class SpeedGauge extends View {
    private static final float MAX = 140f;

    private final Paint track = stroke(Ui.TRACK), fill = stroke(Ui.ACCENT);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG), unit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int speed;

    SpeedGauge(Context c) {
        super(c);
        num.setTypeface(Ui.bold);
        num.setTextSize(Ui.pxf(62));
        num.setColor(Ui.TEXT);
        num.setTextAlign(Paint.Align.CENTER);
        unit.setTypeface(Ui.medium);
        unit.setTextSize(Ui.pxf(18));
        unit.setColor(Ui.TEXT2);
        unit.setTextAlign(Paint.Align.CENTER);
    }

    private static Paint stroke(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(Ui.pxf(12));
        p.setColor(color);
        return p;
    }

    void setSpeed(int s) {
        if (s == speed) return;
        speed = s;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), sw = Ui.pxf(12);
        float r = Math.min(w, h) / 2f - sw, cx = w / 2f, cy = h / 2f;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(oval, 135, 270, false, track);
        float sweep = 270f * Math.min(1f, speed / MAX);
        if (sweep > 0) c.drawArc(oval, 135, sweep, false, fill);
        c.drawText(String.valueOf(speed), cx, cy + Ui.pxf(18), num);
        c.drawText("km/j", cx, cy + Ui.pxf(48), unit);
    }
}
