package com.goldendash.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
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

    static SliderCard volume(final Context c) {
        final AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        final int st = AudioManager.STREAM_MUSIC;
        return new SliderCard(c, R.drawable.ic_volume, "Volume", new Source() {
            private BroadcastReceiver rx;

            @Override public float get() {
                int max = am.getStreamMaxVolume(st);
                return max <= 0 ? 0 : am.getStreamVolume(st) / (float) max;
            }

            @Override public void set(float v) {
                int idx = Math.round(v * am.getStreamMaxVolume(st));
                if (idx != am.getStreamVolume(st)) am.setStreamVolume(st, idx, 0);
            }

            @Override public void release() { }

            @Override public void watch(final Runnable changed) {
                rx = new BroadcastReceiver() {
                    @Override public void onReceive(Context ctx, Intent i) {
                        changed.run();
                    }
                };
                // Siaran tersembunyi tapi dipakai luas; dikirim tiap volume berubah, termasuk dari tombol setir.
                c.registerReceiver(rx, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
            }

            @Override public void unwatch() {
                if (rx != null) c.unregisterReceiver(rx);
                rx = null;
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
