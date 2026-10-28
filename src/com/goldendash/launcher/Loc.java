package com.goldendash.launcher;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

/** GPS untuk speedometer dan cuaca. Hanya aktif selama home di depan (lihat HomeActivity.onResume/onPause). */
final class Loc implements LocationListener {
    interface Listener {
        void onLocation(Location l);
    }

    private static Loc instance;

    private final Context ctx;
    private final LocationManager lm;
    private final List<Listener> listeners = new ArrayList<>();
    private Location last;
    private boolean running;

    static Loc get(Context c) {
        if (instance == null) instance = new Loc(c.getApplicationContext());
        return instance;
    }

    private Loc(Context c) {
        ctx = c;
        lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
    }

    static boolean allowed(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void add(Listener l) {
        listeners.add(l);
        if (last != null) l.onLocation(last);
    }

    void remove(Listener l) {
        listeners.remove(l);
    }

    Location last() {
        return last;
    }

    @SuppressWarnings("MissingPermission")
    void start() {
        if (running || lm == null || !allowed(ctx)) return;
        running = true;
        List<String> all = lm.getAllProviders();
        try {
            if (all.contains(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
        } catch (RuntimeException ignored) { }
        try {
            if (all.contains(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60_000, 100, this, Looper.getMainLooper());
        } catch (RuntimeException ignored) { }
        for (String p : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (last == null || l.getTime() > last.getTime())) last = l;
            } catch (RuntimeException ignored) { }
        }
        if (last != null) dispatch();
    }

    void stop() {
        if (!running) return;
        running = false;
        try {
            lm.removeUpdates(this);
        } catch (RuntimeException ignored) { }
    }

    /** Kecepatan dari fix GPS terakhir; 0 bila belum ada fix atau fix-nya sudah lebih dari 4 detik. */
    int speedKmh() {
        Location l = last;
        if (l == null || !l.hasSpeed() || !LocationManager.GPS_PROVIDER.equals(l.getProvider())) return 0;
        if (ageMs(l) > 4000) return 0;
        float kmh = l.getSpeed() * 3.6f;
        return kmh < 2f ? 0 : Math.round(kmh);
    }

    private static long ageMs(Location l) {
        return (SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 1_000_000L;
    }

    @Override public void onLocationChanged(Location l) {
        // Fix jaringan tidak menimpa fix GPS yang masih baru (supaya speedometer tidak sempat jatuh ke 0).
        boolean gps = LocationManager.GPS_PROVIDER.equals(l.getProvider());
        if (!gps && last != null && LocationManager.GPS_PROVIDER.equals(last.getProvider()) && ageMs(last) < 10_000) return;
        last = l;
        dispatch();
    }

    private void dispatch() {
        for (Listener x : new ArrayList<>(listeners)) x.onLocation(last);
    }

    // Wajib diimplementasikan: di Android 10 ke bawah ketiganya belum punya implementasi bawaan.
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override public void onProviderEnabled(String provider) { }

    @Override public void onProviderDisabled(String provider) { }
}
