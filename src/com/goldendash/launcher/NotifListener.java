package com.goldendash.launcher;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

/**
 * Akses notifikasi dipakai untuk dua hal saja: membaca sesi media aplikasi lain (Media) dan membaca notifikasi
 * navigasi Google Maps/Waze untuk petunjuk belokan di home (Nav). Notifikasi aplikasi lain tidak dibaca.
 */
public final class NotifListener extends NotificationListenerService {
    static final String[] NAV_PKGS = {"com.google.android.apps.maps", "com.waze", "com.sygic.aura", "net.osmand"};

    @Override public void onListenerConnected() {
        Media.get(this).start();
        scan();
    }

    @Override public void onListenerDisconnected() {
        Nav.clear();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (isNav(sbn.getPackageName())) scan();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (isNav(sbn.getPackageName())) scan();
    }

    private static boolean isNav(String pkg) {
        for (String p : NAV_PKGS) if (p.equals(pkg)) return true;
        return false;
    }

    private void scan() {
        StatusBarNotification[] all;
        try {
            all = getActiveNotifications();
        } catch (RuntimeException e) {
            return;
        }
        if (all != null) {
            for (StatusBarNotification sbn : all) {
                if (!isNav(sbn.getPackageName())) continue;
                Notification n = sbn.getNotification();
                boolean ongoing = (n.flags & Notification.FLAG_ONGOING_EVENT) != 0;
                if (!ongoing && !"navigation".equals(n.category)) continue;
                Bundle e = n.extras;
                if (e == null || e.containsKey(Notification.EXTRA_MEDIA_SESSION)) continue;
                String title = str(e.getCharSequence(Notification.EXTRA_TITLE));
                String text = str(e.getCharSequence(Notification.EXTRA_TEXT));
                if (title == null && text == null) continue;
                // Jarak ("300 m") ditampilkan besar, petunjuknya di bawah, apa pun urutan dari aplikasinya.
                if (text != null && isDistance(text) && (title == null || !isDistance(title))) {
                    String t = title;
                    title = text;
                    text = t;
                }
                String footer = str(e.getCharSequence(Notification.EXTRA_SUB_TEXT));
                Nav.set(title != null ? title : text, title != null ? text : null, footer, largeIcon(n), n.contentIntent);
                return;
            }
        }
        Nav.clear();
    }

    private static String str(CharSequence s) {
        return TextUtils.isEmpty(s) ? null : s.toString().trim();
    }

    private static boolean isDistance(String s) {
        return s.matches("^\\s*[\\d.,]+\\s?(m|km|mi|ft|yd)\\b.*");
    }

    private Bitmap largeIcon(Notification n) {
        try {
            Icon ic = n.getLargeIcon();
            if (ic != null) {
                Drawable d = ic.loadDrawable(this);
                if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();
                if (d != null) {
                    Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                    d.setBounds(0, 0, 128, 128);
                    d.draw(new Canvas(b));
                    return b;
                }
            }
            Object o = n.extras.get(Notification.EXTRA_LARGE_ICON);
            if (o instanceof Bitmap) return (Bitmap) o;
        } catch (RuntimeException ignored) { }
        return null;
    }
}
