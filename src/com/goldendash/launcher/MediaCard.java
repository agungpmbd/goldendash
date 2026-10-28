package com.goldendash.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Kartu pemutar: sampul, aplikasi, judul, artis, progres, dan kontrol. Progres hanya berdetak saat terlihat & diputar. */
final class MediaCard extends FrameLayout implements Media.Listener {
    private final ImageView art, appIcon, heart;
    private final TextView appName, status, title, artist, pos, dur;
    private final Bar bar;
    private final Controls controls;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean visible;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            showProgress();
            if (visible && Media.get(getContext()).playing()) h.postDelayed(this, 1000);
        }
    };

    MediaCard(Context c) {
        super(c);
        setBackground(Ui.card());
        art = Ui.add(this, new ImageView(c), 20, 20, 184, 232);
        Ui.clip(art, 16);
        appIcon = Ui.add(this, new ImageView(c), 224, 22, 36, 36);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appName = Ui.add(this, Ui.text(c, 17, Ui.medium, Ui.TEXT), 270, 20, 360, 22);
        status = Ui.add(this, Ui.text(c, 13, Ui.regular, Ui.TEXT2), 270, 44, 360, 18);
        heart = Ui.add(this, Ui.icon(c, R.drawable.ic_heart, Ui.TEXT), 644, 20, 40, 40);
        heart.setPadding(Ui.px(5), Ui.px(5), Ui.px(5), Ui.px(5));
        Ui.pressable(heart, -1);
        heart.setOnClickListener(v -> Media.get(c).like());
        title = Ui.add(this, Ui.text(c, 30, Ui.bold, Ui.TEXT), 224, 82, 460, 38);
        artist = Ui.add(this, Ui.text(c, 18, Ui.regular, Ui.TEXT2), 224, 124, 460, 24);
        bar = Ui.add(this, new Bar(c), 224, 160, 460, 8);
        pos = Ui.add(this, Ui.text(c, 14, Ui.regular, Ui.TEXT2), 224, 174, 120, 18);
        dur = Ui.add(this, Ui.text(c, 14, Ui.regular, Ui.TEXT2), 564, 174, 120, 18);
        dur.setGravity(Gravity.END);
        float cw = Controls.width(66, 40, 52);
        controls = Ui.add(this, new Controls(c, 66, 40, 52), 454 - cw / 2f, 188, cw, 66);
        setOnClickListener(v -> open());
        show();
    }

    private void open() {
        Context c = getContext();
        if (!Media.allowed(c)) {
            Ui.start(c, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            return;
        }
        Media m = Media.get(c);
        Apps.App a = m.has() ? Apps.get(c).find(m.pkg()) : null;
        if (a != null) Apps.get(c).launch(c, a);
        else Apps.get(c).launchAny(c, Sidebar.MUSIC);
    }

    private void show() {
        Context c = getContext();
        Media m = Media.get(c);
        Apps.App app = m.has() ? Apps.get(c).find(m.pkg()) : Apps.get(c).find(Sidebar.MUSIC);
        if (app != null && app.icon != null) {
            appIcon.setImageTintList(null);
            appIcon.setBackground(null);
            appIcon.setPadding(0, 0, 0, 0);
            appIcon.setImageBitmap(app.icon);
        } else {
            appIcon.setImageResource(R.drawable.ic_music);
            appIcon.setImageTintList(ColorStateList.valueOf(Ui.WHITE));
            appIcon.setBackground(Ui.accent(-1));
            int pad = Ui.px(8);
            appIcon.setPadding(pad, pad, pad, pad);
        }
        appName.setText(app != null ? app.label : "Musik");
        heart.setVisibility(View.GONE);
        if (!Media.allowed(c)) {
            status.setText("Pemutar");
            title.setText("Izinkan akses notifikasi");
            artist.setText("Ketuk di sini supaya musik yang diputar tampil");
            placeholderArt();
        } else if (!m.has()) {
            status.setText("Tidak ada yang diputar");
            title.setText("Belum ada musik");
            artist.setText("Ketuk ▶ untuk membuka pemutar musik");
            placeholderArt();
        } else {
            status.setText(m.playing() ? "Now Playing" : "Dijeda");
            String t = m.title(), a = m.artist();
            title.setText(t != null ? t : app != null ? app.label : "Memutar");
            artist.setText(a != null ? a : "");
            Bitmap b = m.art();
            if (b != null) {
                art.setImageTintList(null);
                art.setScaleType(ImageView.ScaleType.CENTER_CROP);
                art.setPadding(0, 0, 0, 0);
                art.setBackground(null);
                art.setImageBitmap(b);
            } else {
                placeholderArt();
            }
            showLike(m);
        }
        controls.setPlaying(m.playing());
        showProgress();
        h.removeCallbacks(tick);
        if (visible && m.playing()) h.postDelayed(tick, 1000);
    }

    /** Ikon "suka" diambil dari aplikasi pemutarnya, jadi status suka/tidak tampil sesuai aplikasi itu. */
    private void showLike(Media m) {
        PlaybackState.CustomAction a = m.likeAction();
        if (a == null) return;
        heart.setVisibility(View.VISIBLE);
        Drawable d = null;
        try {
            if (a.getIcon() != 0) d = getContext().getPackageManager().getResourcesForApplication(m.pkg()).getDrawable(a.getIcon(), null);
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) { }
        if (d != null) heart.setImageDrawable(d);
        else heart.setImageResource(R.drawable.ic_heart);
        heart.setImageTintList(ColorStateList.valueOf(Ui.TEXT));
    }

    private void placeholderArt() {
        art.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.dark ? new int[] {0xFF5A5DA8, 0xFF25263F} : new int[] {0xFFF0D3A8, 0xFFC4936A}));
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Ui.px(62);
        art.setPadding(pad, pad, pad, pad);
        art.setImageResource(R.drawable.ic_music);
        art.setImageTintList(ColorStateList.valueOf(0xE6FFFFFF));
    }

    private void showProgress() {
        Media m = Media.get(getContext());
        long d = m.has() ? m.duration() : 0, p = m.has() ? m.position() : 0;
        bar.set(d > 0 ? p / (float) d : 0f);
        pos.setText(Ui.mmss(p));
        dur.setText(d > 0 ? Ui.mmss(d) : "");
    }

    @Override public void onMediaChanged() {
        show();
    }

    @Override public void onVisibilityAggregated(boolean v) {
        super.onVisibilityAggregated(v);
        visible = v;
        if (v) show();
        else h.removeCallbacks(tick);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Media.get(getContext()).add(this);
        show();
    }

    @Override protected void onDetachedFromWindow() {
        Media.get(getContext()).remove(this);
        h.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    /** Garis progres lagu. */
    static final class Bar extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;

        Bar(Context c) {
            super(c);
            track.setColor(Ui.TRACK);
            fill.setColor(Ui.ACCENT);
        }

        void set(float v) {
            v = Math.max(0f, Math.min(1f, v));
            if (v == value) return;
            value = v;
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = h / 2f;
            c.drawRoundRect(0, 0, w, h, r, r, track);
            if (value > 0) c.drawRoundRect(0, 0, Math.max(h, w * value), h, r, r, fill);
        }
    }
}
