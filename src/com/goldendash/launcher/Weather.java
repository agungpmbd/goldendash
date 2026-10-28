package com.goldendash.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * Cuaca sekarang dari Open-Meteo (gratis, tanpa API key) dan nama kota dari Geocoder. Diperbarui paling sering tiap
 * 30 menit atau saat pindah lebih dari 10 km; hasil terakhir disimpan supaya langsung tampil setelah headunit menyala.
 */
final class Weather {
    static final int NONE = -1, SUN = 0, PARTLY = 1, CLOUD = 2, FOG = 3, RAIN = 4, STORM = 5, SNOW = 6;
    static final int NO_TEMP = Integer.MIN_VALUE;

    static String city;
    static int temp = NO_TEMP;
    static int code = -1;
    static boolean day = true;

    private static long at, lastTry;
    private static double lat = Double.NaN, lon = Double.NaN;
    private static boolean busy, loaded;

    private Weather() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    static void load(Context c) {
        if (loaded) return;
        loaded = true;
        SharedPreferences p = prefs(c);
        city = p.getString("city", null);
        temp = p.getInt("temp", NO_TEMP);
        code = p.getInt("code", -1);
        day = p.getBoolean("day", true);
        at = p.getLong("at", 0);
        lat = Double.longBitsToDouble(p.getLong("lat", Double.doubleToLongBits(Double.NaN)));
        lon = Double.longBitsToDouble(p.getLong("lon", Double.doubleToLongBits(Double.NaN)));
    }

    static void update(Context c, Location l, final Runnable done) {
        load(c);
        if (busy || l == null) return;
        long now = System.currentTimeMillis();
        final boolean moved = Double.isNaN(lat) || distance(l) > 10_000;
        boolean fresh = at > 0 && at <= now && now - at < 30 * 60_000L;
        if (!moved && fresh) return;
        // Batasi percobaan: 5 menit bila tidak pindah (mis. sedang tanpa internet), 1 menit bila sudah pindah kota.
        long gap = moved ? 60_000L : 5 * 60_000L;
        if (lastTry > 0 && Math.abs(now - lastTry) < gap) return;
        lastTry = now;
        busy = true;
        final Context app = c.getApplicationContext();
        final double la = l.getLatitude(), lo = l.getLongitude();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            final String name = geocode(app, la, lo);
            int t = NO_TEMP, cd = -1;
            boolean d = true, ok = false;
            try {
                String url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,weather_code,is_day&timezone=auto", la, lo);
                JSONObject cur = new JSONObject(get(url)).getJSONObject("current");
                t = (int) Math.round(cur.getDouble("temperature_2m"));
                cd = cur.getInt("weather_code");
                d = cur.optInt("is_day", 1) == 1;
                ok = true;
            } catch (Exception ignored) { }
            final int ft = t, fc = cd;
            final boolean fd = d, fok = ok;
            main.post(() -> {
                busy = false;
                // Nama kota lama tidak boleh menempel di cuaca lokasi baru.
                if (name != null) city = name;
                else if (moved && fok) city = null;
                if (fok) {
                    temp = ft;
                    code = fc;
                    day = fd;
                    at = System.currentTimeMillis();
                    lat = la;
                    lon = lo;
                }
                prefs(app).edit().putString("city", city).putInt("temp", temp).putInt("code", code)
                        .putBoolean("day", day).putLong("at", at)
                        .putLong("lat", Double.doubleToLongBits(lat)).putLong("lon", Double.doubleToLongBits(lon)).apply();
                done.run();
            });
        }, "weather").start();
    }

    private static float distance(Location l) {
        float[] r = new float[1];
        Location.distanceBetween(lat, lon, l.getLatitude(), l.getLongitude(), r);
        return r[0];
    }

    private static String geocode(Context c, double la, double lo) {
        if (!Geocoder.isPresent()) return null;
        try {
            List<Address> list = new Geocoder(c, new Locale("id", "ID")).getFromLocation(la, lo, 1);
            if (list == null || list.isEmpty()) return null;
            Address a = list.get(0);
            String n = a.getLocality();
            if (TextUtils.isEmpty(n)) n = a.getSubAdminArea();
            if (TextUtils.isEmpty(n)) n = a.getAdminArea();
            if (TextUtils.isEmpty(n)) return null;
            return n.replaceFirst("^(Kota|Kabupaten|Kab\\.|Kecamatan)\\s+", "");
        } catch (Exception e) {
            return null;
        }
    }

    private static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(10_000);
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    /** Kode cuaca WMO → bentuk ikon. */
    static int kind() {
        int c = code;
        if (c < 0) return NONE;
        if (c <= 1) return SUN;
        if (c == 2) return PARTLY;
        if (c == 3) return CLOUD;
        if (c == 45 || c == 48) return FOG;
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return SNOW;
        if (c >= 95) return STORM;
        if (c >= 51) return RAIN;
        return CLOUD;
    }

    static String desc() {
        int c = code;
        if (c < 0) return null;
        if (c == 0) return "Cerah";
        if (c == 1) return "Cerah";
        if (c == 2) return "Cerah berawan";
        if (c == 3) return "Berawan";
        if (c == 45 || c == 48) return "Berkabut";
        if (c >= 51 && c <= 57) return "Gerimis";
        if (c == 61 || c == 80) return "Hujan ringan";
        if (c == 63 || c == 81) return "Hujan sedang";
        if (c == 65 || c == 82) return "Hujan lebat";
        if (c == 66 || c == 67) return "Hujan";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "Salju";
        if (c >= 95) return "Hujan petir";
        return "Berawan";
    }
}
