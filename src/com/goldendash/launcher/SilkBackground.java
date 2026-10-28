package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Latar layar, digambar sekali ke bitmap saat ukuran atau tema berubah, jadi tidak memakan CPU saat diam.
 * Tema Terang: kain sutra krem keemasan. Tema Gelap: wallpaper res/drawable-nodpi/wall_dark.jpg bila ada (tidak
 * ikut di repo), kalau tidak ada: lipatan kain biru-ungu gelap dengan tepi bercahaya.
 * Tiap lipatan adalah kurva Bézier yang diiris tipis-tipis dengan sorot terang dan bayangan.
 */
final class SilkBackground extends View {
    /** {y kiri, y kontrol 1, y kontrol 2, y kanan, tebal}, semuanya pecahan tinggi layar. */
    private static final float[][] FOLDS = {
        {0.22f, -0.34f, 0.42f, -0.26f, 0.24f},
        {0.62f, 0.02f, 0.78f, -0.06f, 0.32f},
        {0.98f, 0.40f, 0.98f, 0.22f, 0.34f},
        {1.28f, 0.86f, 1.12f, 0.56f, 0.32f},
    };
    private static final int STEPS = 72;

    private Bitmap bmp;

    SilkBackground(Context c) {
        super(c);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        render(w, h);
    }

    /** Gambar ulang untuk tema yang sedang aktif. */
    void refresh() {
        render(getWidth(), getHeight());
        invalidate();
    }

    private void render(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (bmp != null) bmp.recycle();
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        if (!Ui.dark) silk(c, w, h, false);
        else if (!wallpaper(c, w, h)) silk(c, w, h, true);
    }

    /** Wallpaper gelap dipotong tengah mengisi layar; false bila file wallpaper tidak disertakan saat build. */
    private boolean wallpaper(Canvas c, int w, int h) {
        // Lewat nama, bukan R.drawable.wall_dark: wallpaper opsional dan build tetap jalan tanpanya.
        int id = getResources().getIdentifier("wall_dark", "drawable", getContext().getPackageName());
        Bitmap src = id == 0 ? null : BitmapFactory.decodeResource(getResources(), id);
        if (src == null) return false;
        float scale = Math.max(w / (float) src.getWidth(), h / (float) src.getHeight());
        float sw = w / scale, sh = h / scale, sx = (src.getWidth() - sw) / 2f, sy = (src.getHeight() - sh) / 2f;
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        c.drawBitmap(src, new Rect(Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh)),
                new RectF(0, 0, w, h), p);
        src.recycle();
        return true;
    }

    @Override protected void onDraw(Canvas c) {
        if (bmp != null) c.drawBitmap(bmp, 0, 0, null);
    }

    private static void silk(Canvas c, int w, int h, boolean dark) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        int[] base = dark
                ? new int[] {0xFF1D1D31, 0xFF121220, 0xFF24243D, 0xFF181A2E}
                : new int[] {0xFFF2E9DC, 0xFFDCC7AC, 0xFFEFE4D5, 0xFFD6BE9F};
        int lightRgb = dark ? 0x8A8CEB : 0xFFFAF2, shadeRgb = dark ? 0x05050C : 0x8A6A48;
        int lightMax = dark ? 110 : 200, shadeMax = dark ? 150 : 105;
        p.setShader(new LinearGradient(0, 0, w, h, base, new float[] {0f, 0.38f, 0.66f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // Tiap lipatan dipotong menjadi irisan tipis yang tidak saling menumpuk (tanpa antialias), supaya gradasinya
        // halus tanpa garis-garis.
        p.setAntiAlias(false);
        p.setStyle(Paint.Style.FILL);
        Path path = new Path();
        float x0 = -0.1f * w, x1 = 0.32f * w, x2 = 0.62f * w, x3 = 1.1f * w;
        for (float[] f : FOLDS) {
            float thick = f[4] * h, step = thick / STEPS;
            for (int i = 0; i < STEPS; i++) {
                float k = (i + 0.5f) / STEPS, a = i * step, b = a + step;
                path.reset();
                path.moveTo(x0, f[0] * h + a);
                path.cubicTo(x1, f[1] * h + a, x2, f[2] * h + a, x3, f[3] * h + a);
                path.lineTo(x3, f[3] * h + b);
                path.cubicTo(x2, f[2] * h + b, x1, f[1] * h + b, x0, f[0] * h + b);
                path.close();
                // Sorot terang di sepertiga atas lipatan, bayangan di dekat tepi bawah.
                int light = Math.round(lightMax * bump(k, 0.34f, 0.17f));
                int shade = Math.round(shadeMax * bump(k, 0.80f, 0.14f));
                if (light > 2) {
                    p.setColor((light << 24) | lightRgb);
                    c.drawPath(path, p);
                }
                if (shade > 2) {
                    p.setColor((shade << 24) | shadeRgb);
                    c.drawPath(path, p);
                }
            }
        }

        if (!dark) return;
        // Tepi lipatan bercahaya tipis, seperti garis periwinkle di wallpaper gelap.
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, h / 480f));
        p.setColor(0xB39EA3FF);
        for (float[] f : FOLDS) {
            path.reset();
            path.moveTo(x0, f[0] * h);
            path.cubicTo(x1, f[1] * h, x2, f[2] * h, x3, f[3] * h);
            c.drawPath(path, p);
        }
    }

    private static float bump(float k, float center, float width) {
        float d = (k - center) / width;
        return (float) Math.exp(-d * d);
    }
}
