package com.goldendash.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Kartu slider Volume dan Kecerahan. Nilainya ikut berubah bila diatur dari tempat lain (tombol fisik, sistem). */
final class SliderCard extends FrameLayout {
    /** Sumber nilai 0..1 yang diatur slider. */
    interface Source {
        float get();

        void set(float v);

        void release();

        void watch(Runnable changed);

        void unwatch();
    }

    private final Source src;
    private final Slider slider;
    private final TextView pct;
    private final Runnable changed = this::pull;

    SliderCard(Context c, int icon, String label, Source source) {
        super(c);
        src = source;
        setBackground(Ui.card());
        Ui.add(this, Ui.icon(c, icon, Ui.TEXT), 20, 25, 36, 36);
        TextView l = Ui.add(this, Ui.text(c, 16, Ui.medium, Ui.TEXT), 72, 14, 200, 22);
        l.setText(label);
        slider = Ui.add(this, new Slider(c), 64, 38, 205, 36);
        pct = Ui.add(this, Ui.text(c, 17, Ui.medium, Ui.TEXT), 273, 40, 56, 32);
        pct.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        slider.listener = new Slider.Listener() {
            @Override public void onChange(float v) {
                src.set(v);
                pct.setText(Math.round(v * 100) + "%");
            }

            @Override public void onRelease() {
                src.release();
            }
        };
        pull();
    }

    private void pull() {
        if (slider.dragging) return;
        float v = Math.max(0f, Math.min(1f, src.get()));
        slider.set(v);
        pct.setText(Math.round(v * 100) + "%");
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        src.watch(changed);
        pull();
    }

    @Override protected void onDetachedFromWindow() {
        src.unwatch();
        super.onDetachedFromWindow();
    }

    /** Volume asli di headunit NWD: nilai knob yang disimpan MCU (aplikasi biasa hanya bisa membacanya). */
    static final String MCU_VOLUME = "mcu_system_volume";
    /** Cara mengubah volume MCU: belum diketahui, tombol volume lewat AudioManager, atau adjustStreamVolume. */
    static final int VIA_UNKNOWN = 0, VIA_KEY = 1, VIA_ADJUST = 2;

    /**
     * Volume. Di Android biasa: volume media (STREAM_MUSIC) diatur langsung. Di headunit NWD volume Android dikunci di
     * maksimum dan volume asli ada di MCU ({@link #MCU_VOLUME}), jadi slider meniru knob: naik/turun selangkah demi
     * selangkah sampai mencapai nilai tujuan. Dua cara dicoba (tombol volume, lalu adjustStreamVolume); cara yang
     * terbukti mengubah volume MCU diingat. Batas atas MCU dipelajari saat volume berhenti naik.
     */
    static SliderCard volume(final Context c) {
        final AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        final ContentResolver cr = c.getContentResolver();
        final SharedPreferences prefs = c.getSharedPreferences("volume", Context.MODE_PRIVATE);
        final Handler h = new Handler(Looper.getMainLooper());
        final int st = AudioManager.STREAM_MUSIC;
        return new SliderCard(c, R.drawable.ic_volume, "Volume", new Source() {
            private BroadcastReceiver rx;
            private ContentObserver obs;
            private Runnable onChange;
            private int target = -1, before, sentVia, tryVia = VIA_KEY;
            private boolean waiting, warned;

            private final Runnable step = this::step;

            private int mcu() {
                return Settings.System.getInt(cr, MCU_VOLUME, -1);
            }

            private int mcuMax() {
                return Math.max(prefs.getInt("max", 40), Math.max(1, mcu()));
            }

            @Override public float get() {
                int v = mcu();
                if (v >= 0) return v / (float) mcuMax();
                int max = am.getStreamMaxVolume(st);
                return max <= 0 ? 0 : am.getStreamVolume(st) / (float) max;
            }

            @Override public void set(float v) {
                if (mcu() < 0) {
                    int idx = Math.round(v * am.getStreamMaxVolume(st));
                    if (idx != am.getStreamVolume(st)) am.setStreamVolume(st, idx, 0);
                    return;
                }
                target = Math.round(v * mcuMax());
                if (!waiting) {
                    h.removeCallbacks(step);
                    h.post(step);
                }
            }

            /** Satu langkah: periksa hasil langkah sebelumnya, lalu kirim langkah berikutnya menuju {@link #target}. */
            private void step() {
                int cur = mcu();
                int via = prefs.getInt("via", VIA_UNKNOWN);
                if (waiting) {
                    waiting = false;
                    if (cur != before) {
                        if (via == VIA_UNKNOWN) prefs.edit().putInt("via", sentVia).apply();
                    } else if (via == VIA_UNKNOWN && sentVia == VIA_KEY) {
                        tryVia = VIA_ADJUST;
                    } else if (via == VIA_UNKNOWN) {
                        target = -1;
                        tryVia = VIA_KEY;
                        warn();
                        // Kembalikan slider ke volume sebenarnya.
                        if (onChange != null) onChange.run();
                        return;
                    } else {
                        // Cara sudah diketahui tapi volume tidak bergerak: sudah di batas.
                        if (target > cur && cur >= 10) prefs.edit().putInt("max", cur).apply();
                        target = -1;
                        return;
                    }
                }
                if (target < 0 || cur < 0 || cur == target) {
                    target = -1;
                    return;
                }
                boolean up = target > cur;
                sentVia = via != VIA_UNKNOWN ? via : tryVia;
                before = cur;
                if (sentVia == VIA_KEY) {
                    int code = up ? KeyEvent.KEYCODE_VOLUME_UP : KeyEvent.KEYCODE_VOLUME_DOWN;
                    long now = SystemClock.uptimeMillis();
                    am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
                    am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
                } else {
                    am.adjustStreamVolume(st, up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                }
                waiting = true;
                // MCU butuh sesaat untuk memperbarui nilainya; tunggu sebelum memeriksa hasil.
                h.postDelayed(step, 250);
            }

            private void warn() {
                if (warned) return;
                warned = true;
                Toast.makeText(c, "Volume headunit ini belum bisa diatur dari GoldenDash. Pakai knob atau tombol setir.",
                        Toast.LENGTH_LONG).show();
            }

            @Override public void release() { }

            @Override public void watch(final Runnable changed) {
                onChange = changed;
                rx = new BroadcastReceiver() {
                    @Override public void onReceive(Context ctx, Intent i) {
                        changed.run();
                    }
                };
                // Siaran tersembunyi tapi dipakai luas; dikirim tiap volume berubah, termasuk dari tombol setir.
                IntentFilter f = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
                f.addAction("com.nwd.action.VOLUME_CHANGED");
                c.registerReceiver(rx, f);
                // Knob NWD: nilai MCU berubah tanpa siaran volume Android.
                obs = new ContentObserver(h) {
                    @Override public void onChange(boolean self) {
                        changed.run();
                    }
                };
                cr.registerContentObserver(Settings.System.getUriFor(MCU_VOLUME), false, obs);
            }

            @Override public void unwatch() {
                if (rx != null) c.unregisterReceiver(rx);
                if (obs != null) cr.unregisterContentObserver(obs);
                rx = null;
                obs = null;
                h.removeCallbacks(step);
                waiting = false;
                target = -1;
            }
        });
    }

    /**
     * Kecerahan sistem butuh izin "Ubah setelan sistem" (./build.sh install memberikannya lewat ADB). Tanpa izin itu
     * slider tetap bekerja, tapi hanya untuk jendela home, dan pengguna diarahkan sekali ke layar izinnya.
     */
    static SliderCard brightness(final Activity a) {
        final ContentResolver cr = a.getContentResolver();
        return new SliderCard(a, R.drawable.ic_brightness, "Kecerahan", new Source() {
            private ContentObserver obs;
            private boolean asked;

            @Override public float get() {
                return Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f;
            }

            @Override public void set(float v) {
                int val = Math.max(5, Math.round(v * 255));
                if (Settings.System.canWrite(a)) {
                    try {
                        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, val);
                    } catch (RuntimeException ignored) { }
                    window(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
                } else {
                    window(val / 255f);
                }
            }

            private void window(float b) {
                WindowManager.LayoutParams lp = a.getWindow().getAttributes();
                if (lp.screenBrightness == b) return;
                lp.screenBrightness = b;
                a.getWindow().setAttributes(lp);
            }

            @Override public void release() {
                if (Settings.System.canWrite(a) || asked) return;
                asked = true;
                Toast.makeText(a, "Izinkan \"Ubah setelan sistem\" supaya kecerahan berlaku di semua aplikasi",
                        Toast.LENGTH_LONG).show();
                Ui.start(a, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + a.getPackageName())));
            }

            @Override public void watch(final Runnable changed) {
                obs = new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override public void onChange(boolean self) {
                        changed.run();
                    }
                };
                cr.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, obs);
            }

            @Override public void unwatch() {
                if (obs != null) cr.unregisterContentObserver(obs);
                obs = null;
            }
        });
    }

    /** Slider: jalur abu-abu, isian biru bergradasi, kenop putih. */
    static final class Slider extends View {
        interface Listener {
            void onChange(float v);

            void onRelease();
        }

        Listener listener;
        boolean dragging;
        private float value;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG), ring = new Paint(Paint.ANTI_ALIAS_FLAG);

        Slider(Context c) {
            super(c);
            track.setColor(Ui.TRACK);
            knob.setColor(Ui.WHITE);
            ring.setColor(0x24000000);
        }

        void set(float v) {
            if (dragging || v == value) return;
            value = v;
            invalidate();
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            fill.setShader(new LinearGradient(0, 0, w, 0, Ui.ACCENT_LIGHT, Ui.ACCENT, Shader.TileMode.CLAMP));
        }

        private float radius() {
            return Ui.pxf(13);
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), cy = getHeight() / 2f, r = radius(), th = Ui.pxf(8);
            float x0 = r, x1 = w - r, x = x0 + (x1 - x0) * value;
            c.drawRoundRect(x0, cy - th / 2, x1, cy + th / 2, th / 2, th / 2, track);
            c.drawRoundRect(x0, cy - th / 2, Math.max(x0 + th, x), cy + th / 2, th / 2, th / 2, fill);
            c.drawCircle(x, cy + Ui.pxf(1), r + Ui.pxf(1.5f), ring);
            c.drawCircle(x, cy, r, knob);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    dragging = true;
                    update(e.getX());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    update(e.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    if (listener != null) listener.onRelease();
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }

        private void update(float x) {
            float r = radius(), v = (x - r) / Math.max(1f, getWidth() - 2 * r);
            v = Math.max(0f, Math.min(1f, v));
            if (v == value) return;
            value = v;
            invalidate();
            if (listener != null) listener.onChange(v);
        }
    }
}
