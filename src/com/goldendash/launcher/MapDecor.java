package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Latar peta hiasan (jalan, taman, kanal, bundaran) untuk panel navigasi, versi terang atau gelap. */
final class MapDecor extends View {
    private Bitmap bmp;

    MapDecor(Context c) {
        super(c);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        if (bmp != null) bmp.recycle();
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        paint(new Canvas(bmp), w, h);
    }

    @Override protected void onDraw(Canvas c) {
        if (bmp != null) c.drawBitmap(bmp, 0, 0, null);
    }

    private static void paint(Canvas c, int w, int h) {
        boolean d = Ui.dark;
        int base = d ? 0xFF1E2033 : 0xFFEDEEEC;
        int park = d ? 0xFF243A35 : 0xFFCFE6C6;
        int water = d ? 0xFF283C63 : 0xFFBDD8F0;
        int road = d ? 0xFF363955 : 0xFFFFFFFF;
        int edge = d ? 0xFF2A2C44 : 0xFFD9DAD8;
        int veil = d ? 0x26000000 : 0x33FFFFFF;

        c.drawColor(base);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(park);
        c.drawRect(w * 0.40f, h * 0.40f, w * 0.54f, h * 0.62f, p);
        c.drawRect(w * 0.84f, h * 0.04f, w * 0.97f, h * 0.30f, p);
        c.drawCircle(w * 0.86f, h * 0.68f, h * 0.12f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(water);
        p.setStrokeWidth(h * 0.035f);
        Path river = new Path();
        river.moveTo(w * 0.30f, -h * 0.1f);
        river.cubicTo(w * 0.42f, h * 0.2f, w * 0.70f, h * 0.18f, w * 1.05f, h * 0.36f);
        c.drawPath(river, p);

        c.save();
        c.rotate(-14, w / 2f, h / 2f);
        p.setColor(road);
        p.setStrokeWidth(Math.max(2f, h * 0.018f));
        float step = h * 0.13f;
        for (float x = -w; x < 2 * w; x += step * 1.4f) c.drawLine(x, -h, x, 2 * h, p);
        for (float y = -h; y < 2 * h; y += step) c.drawLine(-w, y, 2 * w, y, p);
        // Jalan utama: tepi lalu isi.
        float[][] roads = {{-w, h * 0.62f, 2 * w, h * 0.62f}, {w * 0.62f, -h, w * 0.62f, 2 * h}, {-w, h * 0.18f, 2 * w, h * 0.18f}};
        for (float[] r : roads) {
            p.setColor(edge);
            p.setStrokeWidth(h * 0.06f);
            c.drawLine(r[0], r[1], r[2], r[3], p);
            p.setColor(road);
            p.setStrokeWidth(h * 0.045f);
            c.drawLine(r[0], r[1], r[2], r[3], p);
        }
        c.restore();

        p.setColor(road);
        p.setStrokeWidth(h * 0.04f);
        c.drawCircle(w * 0.86f, h * 0.68f, h * 0.15f, p);
        // Selubung tipis supaya kotak petunjuk dan teks tetap mudah dibaca.
        p.setStyle(Paint.Style.FILL);
        p.setColor(veil);
        c.drawRect(0, 0, w, h, p);
    }
}
