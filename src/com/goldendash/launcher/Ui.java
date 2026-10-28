package com.goldendash.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Satuan, warna, dan pembuat view bersama. Semua posisi ditulis dalam satuan desain 1280×720 (layar NWD K2401) lalu
 * diskalakan ke layar sebenarnya, jadi tata letaknya mengikuti mockup di layar mana pun.
 *
 * Warna adalah token tema (Terang/Gelap, lihat {@link #theme}). View membaca token saat dibuat, jadi ganti tema =
 * bangun ulang tampilan (HomeActivity.applyTheme).
 */
final class Ui {
    static final float W = 1280f, H = 720f;
    static final int WHITE = 0xFFFFFFFF;

    static final String LIGHT = "light", DARK = "dark", AUTO = "auto";

    static boolean dark;
    static int TEXT, TEXT2, TEXT3, ACCENT, ACCENT_LIGHT, TRACK, OFF, LINE, STROKE, RIPPLE;
    /** Gradasi atas/bawah: kartu, panel dalam, kolom (cari, tab, chip), kotak di atas peta, tombol putar, sidebar, item aktif. */
    static int CARD_T, CARD_B, INNER_T, INNER_B, FIELD_T, FIELD_B, OVER_T, OVER_B, BUTTON_T, BUTTON_B;
    static int SIDE_A, SIDE_B, SELECT_T, SELECT_B, SELECT_FG, CHIP, WINDOW;
    static int DIALOG;

    static float s = 1f;
    static Typeface regular = Typeface.DEFAULT, medium = Typeface.DEFAULT, bold = Typeface.DEFAULT_BOLD;

    static {
        theme(false);
    }

    private Ui() { }

    static void theme(boolean d) {
        dark = d;
        if (d) {
            // Gelap: kaca biru-ungu gelap di atas wallpaper, aksen periwinkle dari garis wallpaper.
            TEXT = 0xFFF1F0F8;
            TEXT2 = 0xFFB3B2CA;
            TEXT3 = 0xFF7F7E99;
            ACCENT = 0xFF7076F0;
            ACCENT_LIGHT = 0xFF9EA3FF;
            TRACK = 0x33FFFFFF;
            OFF = 0x4DFFFFFF;
            LINE = 0x26FFFFFF;
            STROKE = 0x2EFFFFFF;
            RIPPLE = 0x33FFFFFF;
            CARD_T = 0x9E23243A;
            CARD_B = 0x8A15162A;
            INNER_T = 0x1FFFFFFF;
            INNER_B = 0x0FFFFFFF;
            FIELD_T = 0x29FFFFFF;
            FIELD_B = 0x14FFFFFF;
            OVER_T = 0xEB23253C;
            OVER_B = 0xE01A1C30;
            BUTTON_T = 0x40FFFFFF;
            BUTTON_B = 0x1FFFFFFF;
            SIDE_A = 0xB3121320;
            SIDE_B = 0x8C121320;
            SELECT_T = 0x597076F0;
            SELECT_B = 0x387076F0;
            SELECT_FG = 0xFFD2D4FF;
            CHIP = 0x1FFFFFFF;
            WINDOW = 0xFF15151F;
            DIALOG = android.R.style.Theme_DeviceDefault_Dialog_Alert;
        } else {
            TEXT = 0xFF16161A;
            TEXT2 = 0xFF5E5850;
            TEXT3 = 0xFF948C82;
            ACCENT = 0xFF1A6DEB;
            ACCENT_LIGHT = 0xFF4C93F7;
            TRACK = 0x29000000;
            OFF = 0x4D000000;
            LINE = 0x26000000;
            STROKE = 0xB3FFFFFF;
            RIPPLE = 0x26000000;
            CARD_T = 0x8FFFFFFF;
            CARD_B = 0x5CFFFFFF;
            INNER_T = 0xA6FFFFFF;
            INNER_B = 0x80FFFFFF;
            FIELD_T = 0xA6FFFFFF;
            FIELD_B = 0x80FFFFFF;
            OVER_T = 0xF5FFFFFF;
            OVER_B = 0xEBFFFFFF;
            BUTTON_T = 0xF2FFFFFF;
            BUTTON_B = 0xCCF4F1EC;
            SIDE_A = 0x8CFFFFFF;
            SIDE_B = 0x66FFFFFF;
            SELECT_T = 0xF2EEF5FE;
            SELECT_B = 0xE6DCE9FC;
            SELECT_FG = 0xFF1A6DEB;
            CHIP = 0x73FFFFFF;
            WINDOW = 0xFFE9DCCB;
            DIALOG = android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        }
    }

    static String themePref(Context c) {
        return c.getSharedPreferences("ui", Context.MODE_PRIVATE).getString("theme", LIGHT);
    }

    static void setThemePref(Context c, String t) {
        c.getSharedPreferences("ui", Context.MODE_PRIVATE).edit().putString("theme", t).apply();
    }

    /** Tema yang seharusnya aktif sekarang; Otomatis = gelap pukul 18.00–06.00. */
    static boolean wantDark(Context c) {
        String t = themePref(c);
        if (AUTO.equals(t)) {
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            return h >= 18 || h < 6;
        }
        return DARK.equals(t);
    }

    static void init(int w, int h) {
        s = Math.min(w / W, h / H);
        regular = Typeface.create("sans-serif", Typeface.NORMAL);
        medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        bold = Typeface.create("sans-serif", Typeface.BOLD);
    }

    static int px(float u) {
        return Math.round(u * s);
    }

    static float pxf(float u) {
        return u * s;
    }

    static FrameLayout.LayoutParams at(float x, float y, float w, float h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(1, px(w)), Math.max(1, px(h)));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        return lp;
    }

    static <T extends View> T add(ViewGroup parent, T v, float x, float y, float w, float h) {
        parent.addView(v, at(x, y, w, h));
        return v;
    }

    static TextView text(Context c, float size, Typeface tf, int color) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, pxf(size));
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    static ImageView icon(Context c, int res, int tint) {
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageResource(res);
        if (tint != 0) iv.setImageTintList(ColorStateList.valueOf(tint));
        return iv;
    }

    /** Panel kaca: tembus pandang, lebih terang di atas, dengan tepi tipis. radius < 0 = lingkaran. */
    static GradientDrawable glass(float radius, int top, int bottom) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {top, bottom});
        shape(g, radius);
        g.setStroke(Math.max(1, px(1.5f)), STROKE);
        return g;
    }

    static GradientDrawable card() {
        return glass(26, CARD_T, CARD_B);
    }

    static GradientDrawable inner() {
        return glass(20, INNER_T, INNER_B);
    }

    /** Kolom cari, bilah tab, tombol sekunder. */
    static GradientDrawable field(float radius) {
        return glass(radius, FIELD_T, FIELD_B);
    }

    /** Kotak yang menumpang di atas peta. */
    static GradientDrawable over(float radius) {
        return glass(radius, OVER_T, OVER_B);
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        shape(g, radius);
        return g;
    }

    /** Isian biru tombol/tab aktif. */
    static GradientDrawable accent(float radius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {ACCENT_LIGHT, ACCENT});
        shape(g, radius);
        return g;
    }

    private static void shape(GradientDrawable g, float radius) {
        if (radius < 0) g.setShape(GradientDrawable.OVAL);
        else g.setCornerRadius(pxf(radius));
    }

    /** Efek tekan (ripple) di atas isi view. */
    static void pressable(View v, float radius) {
        v.setForeground(new RippleDrawable(ColorStateList.valueOf(RIPPLE), null, round(WHITE, radius)));
        v.setClickable(true);
    }

    /** Potong isi view menjadi persegi bersudut bulat. */
    static void clip(View v, final float radius) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), pxf(radius));
            }
        });
        v.setClipToOutline(true);
    }

    static boolean start(Context c, Intent i) {
        try {
            if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (RuntimeException e) {
            Toast.makeText(c, "Tidak bisa dibuka di headunit ini", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    static String mmss(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
