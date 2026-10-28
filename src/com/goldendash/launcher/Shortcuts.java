package com.goldendash.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Enam pintasan di home. Slot yang belum pernah diatur memakai aplikasi bawaan yang cocok (Maps, Spotify, Telepon,
 * Radio, Bluetooth, Pengaturan); "" berarti sengaja dikosongkan.
 */
final class Shortcuts {
    static final int N = 6;

    private Shortcuts() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("shortcuts", Context.MODE_PRIVATE);
    }

    static Apps.App get(Context c, int i) {
        Apps apps = Apps.get(c);
        String k = prefs(c).getString("s" + i, null);
        if (k != null) return k.isEmpty() ? null : apps.byKey(k);
        return fallback(apps, i);
    }

    static void set(Context c, int i, Apps.App a) {
        prefs(c).edit().putString("s" + i, a == null ? "" : a.key()).apply();
    }

    private static Apps.App fallback(Apps apps, int i) {
        Apps.App a;
        switch (i) {
            case 0:
                return apps.find(Sidebar.MAPS);
            case 1:
                return apps.find(Sidebar.MUSIC);
            case 2:
                a = apps.findLabel("telepon", "phone", "dialer", "panggilan");
                return a != null ? a : apps.find("com.android.dialer", "com.google.android.dialer");
            case 3:
                return apps.findLabel("radio", "fm");
            case 4:
                return apps.findLabel("bluetooth", "bt");
            case 5:
                return apps.find("com.android.settings");
            default:
                return null;
        }
    }
}
