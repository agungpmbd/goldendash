package com.goldendash.launcher;

import android.app.PendingIntent;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/** Petunjuk belokan terakhir dari notifikasi navigasi Google Maps/Waze (diisi NotifListener). */
final class Nav {
    interface Listener {
        void onNavChanged();
    }

    static boolean active;
    static String primary, secondary, footer;
    static Bitmap icon;
    static PendingIntent open;

    private static final List<Listener> listeners = new ArrayList<>();

    private Nav() { }

    static void add(Listener l) {
        listeners.add(l);
    }

    static void remove(Listener l) {
        listeners.remove(l);
    }

    static void set(String p, String s, String f, Bitmap i, PendingIntent o) {
        active = true;
        primary = p;
        secondary = s;
        footer = f;
        icon = i;
        open = o;
        changed();
    }

    static void clear() {
        if (!active) return;
        active = false;
        primary = secondary = footer = null;
        icon = null;
        open = null;
        changed();
    }

    private static void changed() {
        for (Listener l : new ArrayList<>(listeners)) l.onNavChanged();
    }
}
