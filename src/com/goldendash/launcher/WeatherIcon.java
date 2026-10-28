package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Ikon cuaca bergaya mockup (matahari kuning, awan putih), digambar langsung tanpa aset gambar. */
final class WeatherIcon extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    WeatherIcon(Context c) {
        super(c);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        int k = Weather.kind();
        boolean day = Weather.day;
        switch (k) {
            case Weather.SUN:
                if (day) sun(c, w * 0.5f, h * 0.5f, h * 0.27f);
                else moon(c, w * 0.5f, h * 0.5f, h * 0.3f);
                break;
            case Weather.PARTLY:
                if (day) sun(c, w * 0.36f, h * 0.36f, h * 0.22f);
                else moon(c, w * 0.38f, h * 0.36f, h * 0.24f);
                cloud(c, w * 0.08f, h * 0.36f, w * 0.9f, h * 0.92f, 0xFFFFFFFF);
                break;
            case Weather.RAIN:
            case Weather.STORM:
            case Weather.SNOW:
                cloud(c, w * 0.08f, h * 0.06f, w * 0.92f, h * 0.66f, 0xFFF4F5F7);
                if (k == Weather.STORM) bolt(c, w, h);
                else drops(c, w, h, k == Weather.SNOW);
                break;
            case Weather.FOG:
                cloud(c, w * 0.1f, h * 0.04f, w * 0.9f, h * 0.6f, 0xFFF4F5F7);
                fog(c, w, h);
                break;
            case Weather.CLOUD:
                cloud(c, w * 0.04f, h * 0.18f, w * 0.96f, h * 0.88f, 0xFFFFFFFF);
                break;
            default:
                cloud(c, w * 0.04f, h * 0.18f, w * 0.96f, h * 0.88f, 0xFFE4E2DE);
        }
    }

    private void sun(Canvas c, float cx, float cy, float r) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(r * 0.16f);
        p.setColor(0xFFFFC21A);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cx + cos * r * 1.32f, cy + sin * r * 1.32f, cx + cos * r * 1.62f, cy + sin * r * 1.62f, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.3f, 0xFFFFE16B, 0xFFFFA90A, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
    }

    private void moon(Canvas c, float cx, float cy, float r) {
        path.reset();
        path.addCircle(cx, cy, r, Path.Direction.CW);
        Path bite = new Path();
        bite.addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW);
        path.op(bite, Path.Op.DIFFERENCE);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(0xFFF2CF5B);
        c.drawPath(path, p);
    }

    /** Awan dari beberapa lingkaran di atas alas membulat, dengan bayangan tipis. */
    private void cloud(Canvas c, float l, float t, float r, float b, int color) {
        float w = r - l, h = b - t;
        path.reset();
        path.addCircle(l + w * 0.32f, t + h * 0.58f, h * 0.28f, Path.Direction.CW);
        path.addCircle(l + w * 0.55f, t + h * 0.42f, h * 0.38f, Path.Direction.CW);
        path.addCircle(l + w * 0.76f, t + h * 0.62f, h * 0.26f, Path.Direction.CW);
        path.addRoundRect(l + w * 0.12f, t + h * 0.55f, l + w * 0.92f, b, h * 0.22f, h * 0.22f, Path.Direction.CW);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(0x1F5A4630);
        c.save();
        c.translate(0, h * 0.05f);
        c.drawPath(path, p);
        c.restore();
        p.setShader(new LinearGradient(0, t, 0, b, color, 0xFFDCE0E6, Shader.TileMode.CLAMP));
        c.drawPath(path, p);
        p.setShader(null);
    }

    private void drops(Canvas c, float w, float h, boolean snow) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(h * 0.06f);
        p.setColor(snow ? 0xFFB9C6D6 : 0xFF4C93F7);
        for (int i = 0; i < 3; i++) {
            float x = w * (0.32f + i * 0.18f), y = h * 0.74f;
            if (snow) c.drawPoint(x, y + h * 0.08f, p);
            else c.drawLine(x, y, x - w * 0.05f, y + h * 0.18f, p);
        }
    }

    private void bolt(Canvas c, float w, float h) {
        path.reset();
        path.moveTo(w * 0.52f, h * 0.58f);
        path.lineTo(w * 0.40f, h * 0.80f);
        path.lineTo(w * 0.50f, h * 0.80f);
        path.lineTo(w * 0.44f, h * 0.98f);
        path.lineTo(w * 0.62f, h * 0.72f);
        path.lineTo(w * 0.52f, h * 0.72f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFC21A);
        c.drawPath(path, p);
    }

    private void fog(Canvas c, float w, float h) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(h * 0.06f);
        p.setColor(0xFFB8B2AA);
        c.drawLine(w * 0.18f, h * 0.76f, w * 0.82f, h * 0.76f, p);
        c.drawLine(w * 0.28f, h * 0.9f, w * 0.72f, h * 0.9f, p);
    }
}
