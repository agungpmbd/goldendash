package com.goldendash.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sesi media yang sedang aktif (Spotify, YouTube Music, radio, ...) untuk kartu pemutar dan pemutar mini.
 * Daftar sesi aplikasi lain hanya bisa dibaca bila NotifListener diberi akses notifikasi.
 */
final class Media implements MediaSessionManager.OnActiveSessionsChangedListener {
    interface Listener {
        void onMediaChanged();
    }

    private static Media instance;

    private final Context ctx;
    private final ComponentName listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private MediaController cur;
    private boolean watching;
    private String last;

    private final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) {
            // Yang ini berhenti: barangkali aplikasi lain sedang memutar.
            if (!playing()) start();
            changed();
        }

        @Override public void onMetadataChanged(MediaMetadata metadata) {
            changed();
        }

        @Override public void onSessionDestroyed() {
            setCur(null);
            start();
        }
    };

    static Media get(Context c) {
        if (instance == null) instance = new Media(c.getApplicationContext());
        return instance;
    }

    private Media(Context c) {
        ctx = c;
        listener = new ComponentName(c, NotifListener.class);
    }

    static boolean allowed(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        if (s == null) return false;
        ComponentName cn = new ComponentName(c, NotifListener.class);
        return s.contains(cn.flattenToString()) || s.contains(cn.flattenToShortString());
    }

    void add(Listener l) {
        listeners.add(l);
    }

    void remove(Listener l) {
        listeners.remove(l);
    }

    /** Mulai mengamati sesi (aman dipanggil berulang, misalnya tiap onResume). */
    void start() {
        if (!allowed(ctx)) {
            setCur(null);
            return;
        }
        MediaSessionManager msm = (MediaSessionManager) ctx.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (msm == null) return;
        try {
            if (!watching) {
                msm.addOnActiveSessionsChangedListener(this, listener, main);
                watching = true;
            }
            onActiveSessionsChanged(msm.getActiveSessions(listener));
        } catch (SecurityException e) {
            watching = false;
            setCur(null);
        }
    }

    @Override public void onActiveSessionsChanged(List<MediaController> list) {
        MediaController pick = null;
        if (list != null) {
            for (MediaController m : list) {
                if (isPlaying(m)) {
                    pick = m;
                    break;
                }
            }
            if (pick == null && cur != null) {
                for (MediaController m : list) {
                    if (m.getSessionToken().equals(cur.getSessionToken())) {
                        pick = m;
                        break;
                    }
                }
            }
            if (pick == null && !list.isEmpty()) pick = list.get(0);
        }
        setCur(pick);
    }

    private void setCur(MediaController m) {
        if (cur != null && m != null && cur.getSessionToken().equals(m.getSessionToken())) {
            changed();
            return;
        }
        if (cur != null) cur.unregisterCallback(cb);
        cur = m;
        if (cur != null) cur.registerCallback(cb, main);
        changed();
    }

    private void changed() {
        if (isPlaying(cur)) remember(cur.getPackageName());
        for (Listener l : new ArrayList<>(listeners)) l.onMediaChanged();
    }

    /** Simpan aplikasi yang terakhir memutar, asal bisa dibuka dari launcher (bukan layanan Bluetooth dsb.). */
    private void remember(String pkg) {
        if (pkg == null || pkg.equals(lastPkg(ctx)) || Apps.get(ctx).find(pkg) == null) return;
        last = pkg;
        ctx.getSharedPreferences("media", Context.MODE_PRIVATE).edit().putString("last", pkg).apply();
    }

    private static String lastPkg(Context c) {
        Media m = get(c);
        if (m.last == null) m.last = c.getSharedPreferences("media", Context.MODE_PRIVATE).getString("last", "");
        return m.last;
    }

    /**
     * Pemutar untuk tombol ▶, kartu pemutar, dan tombol Musik saat tidak ada sesi: aplikasi yang terakhir memutar
     * musik, lalu pemutar populer, lalu aplikasi berlabel "musik" (pemutar bawaan headunit).
     */
    static Apps.App player(Context c) {
        Apps a = Apps.get(c);
        Apps.App p = a.find(lastPkg(c));
        if (p == null) p = a.find(Sidebar.MUSIC);
        if (p == null) p = a.findLabel("musik", "music");
        return p;
    }

    static boolean openPlayer(Context c) {
        Apps.App p = player(c);
        return p != null && Apps.get(c).launch(c, p);
    }

    boolean has() {
        return cur != null;
    }

    String pkg() {
        return cur == null ? null : cur.getPackageName();
    }

    private MediaMetadata meta() {
        return cur == null ? null : cur.getMetadata();
    }

    String title() {
        return first(MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
    }

    String artist() {
        return first(MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST, MediaMetadata.METADATA_KEY_ALBUM);
    }

    private String first(String... keys) {
        MediaMetadata m = meta();
        if (m == null) return null;
        for (String k : keys) {
            CharSequence v = m.getText(k);
            if (!TextUtils.isEmpty(v)) return v.toString();
        }
        return null;
    }

    Bitmap art() {
        MediaMetadata m = meta();
        if (m == null) return null;
        String[] keys = {MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON};
        for (String k : keys) {
            Bitmap b = m.getBitmap(k);
            if (b != null) return b;
        }
        return null;
    }

    long duration() {
        MediaMetadata m = meta();
        return m == null ? 0 : Math.max(0, m.getLong(MediaMetadata.METADATA_KEY_DURATION));
    }

    long position() {
        PlaybackState s = cur == null ? null : cur.getPlaybackState();
        if (s == null) return 0;
        long pos = s.getPosition();
        if (s.getState() == PlaybackState.STATE_PLAYING && s.getLastPositionUpdateTime() > 0) {
            pos += (long) ((SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed());
        }
        long d = duration();
        return Math.max(0, d > 0 ? Math.min(pos, d) : pos);
    }

    boolean playing() {
        return isPlaying(cur);
    }

    private static boolean isPlaying(MediaController m) {
        PlaybackState s = m == null ? null : m.getPlaybackState();
        return s != null && (s.getState() == PlaybackState.STATE_PLAYING || s.getState() == PlaybackState.STATE_BUFFERING);
    }

    void playPause() {
        if (cur == null) return;
        if (playing()) cur.getTransportControls().pause();
        else cur.getTransportControls().play();
    }

    void next() {
        if (cur != null) cur.getTransportControls().skipToNext();
    }

    void prev() {
        if (cur != null) cur.getTransportControls().skipToPrevious();
    }

    /** Tombol "suka" milik aplikasi pemutar (mis. Simpan ke Lagu yang Disukai di Spotify), bila ada. */
    PlaybackState.CustomAction likeAction() {
        PlaybackState s = cur == null ? null : cur.getPlaybackState();
        if (s == null) return null;
        for (PlaybackState.CustomAction a : s.getCustomActions()) {
            String n = (a.getAction() + " " + a.getName()).toLowerCase(Locale.ROOT);
            if (n.contains("dislike") || n.contains("thumbs_down") || n.contains("thumb_down")) continue;
            if (n.contains("like") || n.contains("favorite") || n.contains("favourite") || n.contains("heart")
                    || n.contains("love") || n.contains("suka") || n.contains("thumb")) return a;
        }
        return null;
    }

    void like() {
        PlaybackState.CustomAction a = likeAction();
        if (a != null) cur.getTransportControls().sendCustomAction(a, null);
    }
}
