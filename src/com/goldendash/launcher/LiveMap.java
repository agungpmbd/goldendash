package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.Location;
import android.view.View;

/**
 * Peta langsung di kotak navigasi: tile OpenStreetMap berpusat di posisi GPS, utara di atas, dengan penanda
 * mobil yang berputar mengikuti arah gerak. Sebelum ada fix GPS view ini transparan, jadi peta hiasan (MapDecor) di
 * belakangnya yang terlihat. Rute Google Maps tidak bisa ditampilkan di sini; petunjuk belokannya digambar DriveCard.
 */
final class LiveMap extends View implements Loc.Listener, Tiles.Listener {
    private static final int ZOOM = 16, TILE = 256;
    /** Posisi mobil di kotak (pecahan lebar/tinggi): sedikit ke kanan bawah supaya tidak tertutup kotak petunjuk. */
    private static final float CAR_X = 0.62f, CAR_Y = 0.56f;

    private final String style = Tiles.OSM;
    private final Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG), ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG), arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint attr = new Paint(Paint.ANTI_ALIAS_FLAG), attrBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int base = Ui.dark ? 0xFF262A33 : 0xFFF2EFE9;
    private final RectF dst = new RectF();
    private final Path arrowPath = new Path();
    private double lat = Double.NaN, lon;
    private float bearing;
    private boolean hasBearing;

    LiveMap(Context c) {
        super(c);
        halo.setColor(Ui.ACCENT & 0x00FFFFFF | 0x33000000);
        ring.setColor(Ui.WHITE);
        dot.setColor(Ui.ACCENT);
        arrow.setColor(Ui.WHITE);
        attr.setColor(Ui.dark ? 0xCCFFFFFF : 0xCC333333);
        attr.setTextSize(Ui.pxf(10));
        attr.setTextAlign(Paint.Align.RIGHT);
        attrBg.setColor(Ui.dark ? 0x80000000 : 0x99FFFFFF);
        if (Ui.dark) tilePaint.setColorFilter(new ColorMatrixColorFilter(darkMatrix()));
    }

    /**
     * Peta gelap dari tile terang: balik warna, putar hue 180° supaya air tetap biru dan taman tetap hijau, lalu
     * redupkan dengan sedikit rona biru-ungu agar serasi dengan tema Gelap.
     */
    private static ColorMatrix darkMatrix() {
        ColorMatrix m = new ColorMatrix(new float[] {
            -1, 0, 0, 0, 255,
            0, -1, 0, 0, 255,
            0, 0, -1, 0, 255,
            0, 0, 0, 1, 0,
        });
        m.postConcat(new ColorMatrix(new float[] {
            -0.574f, 1.430f, 0.144f, 0, 0,
            0.426f, 0.430f, 0.144f, 0, 0,
            0.426f, 1.430f, -0.856f, 0, 0,
            0, 0, 0, 1, 0,
        }));
        m.postConcat(new ColorMatrix(new float[] {
            0.72f, 0, 0, 0, 12,
            0, 0.74f, 0, 0, 14,
            0, 0, 0.86f, 0, 26,
            0, 0, 0, 1, 0,
        }));
        return m;
    }

    boolean hasFix() {
        return !Double.isNaN(lat);
    }

    @Override public void onLocation(Location l) {
        if (l == null) return;
        boolean moved = Double.isNaN(lat) || Math.abs(l.getLatitude() - lat) > 2e-6 || Math.abs(l.getLongitude() - lon) > 2e-6;
        // Arah dari GPS hanya bermakna saat bergerak; saat diam pertahankan arah terakhir.
        boolean turn = l.hasBearing() && l.getSpeed() > 1f && Math.abs(l.getBearing() - bearing) > 3f;
        if (!moved && !turn) return;
        lat = l.getLatitude();
        lon = l.getLongitude();
        if (l.hasBearing() && l.getSpeed() > 1f) {
            bearing = l.getBearing();
            hasBearing = true;
        }
        invalidate();
    }

    @Override public void onTile() {
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        if (Double.isNaN(lat)) return;
        float w = getWidth(), h = getHeight(), ts = Ui.pxf(TILE);
        int n = 1 << ZOOM;
        // Proyeksi Web Mercator: posisi dalam satuan tile.
        double rad = Math.toRadians(lat);
        double tx = (lon + 180.0) / 360.0 * n;
        double ty = (1.0 - Math.log(Math.tan(rad) + 1.0 / Math.cos(rad)) / Math.PI) / 2.0 * n;
        float cx = w * CAR_X, cy = h * CAR_Y;

        c.drawColor(base);
        Tiles tiles = Tiles.get(getContext());
        int x0 = (int) Math.floor(tx - cx / ts), x1 = (int) Math.floor(tx + (w - cx) / ts);
        int y0 = (int) Math.floor(ty - cy / ts), y1 = (int) Math.floor(ty + (h - cy) / ts);
        for (int y = y0; y <= y1; y++) {
            if (y < 0 || y >= n) continue;
            for (int x = x0; x <= x1; x++) {
                Bitmap b = tiles.get(style, ZOOM, ((x % n) + n) % n, y);
                if (b == null) continue;
                float left = cx + (float) ((x - tx) * ts), top = cy + (float) ((y - ty) * ts);
                dst.set(left, top, left + ts, top + ts);
                c.drawBitmap(b, null, dst, tilePaint);
            }
        }

        // Penanda mobil: lingkaran biru bertepi putih dengan panah arah gerak.
        c.drawCircle(cx, cy, Ui.pxf(24), halo);
        c.drawCircle(cx, cy, Ui.pxf(14), ring);
        c.drawCircle(cx, cy, Ui.pxf(11), dot);
        if (hasBearing) {
            float r = Ui.pxf(7);
            arrowPath.reset();
            arrowPath.moveTo(0, -r);
            arrowPath.lineTo(r * 0.75f, r * 0.8f);
            arrowPath.lineTo(0, r * 0.4f);
            arrowPath.lineTo(-r * 0.75f, r * 0.8f);
            arrowPath.close();
            c.save();
            c.translate(cx, cy);
            c.rotate(bearing);
            c.drawPath(arrowPath, arrow);
            c.restore();
        } else {
            c.drawCircle(cx, cy, Ui.pxf(4), arrow);
        }

        // Atribusi wajib OpenStreetMap, kecil di pojok kanan atas.
        float tw = attr.measureText(Tiles.ATTRIBUTION), pad = Ui.pxf(5), th = Ui.pxf(14);
        dst.set(w - tw - pad * 2 - Ui.pxf(6), Ui.pxf(6), w - Ui.pxf(6), Ui.pxf(6) + th);
        c.drawRoundRect(dst, Ui.pxf(4), Ui.pxf(4), attrBg);
        c.drawText(Tiles.ATTRIBUTION, w - Ui.pxf(6) - pad, Ui.pxf(6) + th - Ui.pxf(4), attr);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Loc.get(getContext()).add(this);
        Tiles.get(getContext()).add(this);
    }

    @Override protected void onDetachedFromWindow() {
        Loc.get(getContext()).remove(this);
        Tiles.get(getContext()).remove(this);
        super.onDetachedFromWindow();
    }
}
