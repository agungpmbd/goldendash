package com.goldendash.launcher;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Menjadikan GoldenDash tampilan utama, atau kembali ke launcher bawaan, tanpa komputer. Firmware NWD mengabaikan
 * pilihan "Aplikasi home" Android saat booting dan memakai property {@link #NWD_PROP}, yang hanya bisa diubah lewat
 * shell; GoldenDash mengubahnya lewat ADB headunit sendiri ({@link LocalAdb}). Di headunit lain, ADB yang sama
 * dipakai untuk set-home-activity; bila ADB tidak aktif, dibuka pengaturan "Aplikasi home" Android.
 */
final class HomeSwitch {
    static final String NWD_PROP = "persist.nwd.launcher.default";

    private HomeSwitch() { }

    static String getprop(String key) {
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"getprop", key});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s = r.readLine();
                return s == null ? "" : s.trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    /** getprop menjalankan proses: cukup sekali per proses, firmware tidak berubah selama berjalan. */
    private static Boolean nwd;

    static boolean isNwd() {
        if (nwd == null) nwd = "true".equals(getprop("ro.nwd.start.launcher.by.fw")) || !getprop(NWD_PROP).isEmpty();
        return nwd;
    }

    static boolean isDefault(Context c) {
        if (isNwd()) return c.getPackageName().equals(getprop(NWD_PROP));
        ResolveInfo r = c.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        return r != null && r.activityInfo != null && c.getPackageName().equals(r.activityInfo.packageName);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ui", Context.MODE_PRIVATE);
    }

    /** {@code done} dipanggil di thread UI setelah percobaan selesai (untuk menyegarkan status di Pengaturan). */
    static void makeDefault(HomeActivity a, Runnable done) {
        String me = a.getPackageName();
        String cmd = "cmd package set-home-activity " + me + "/.HomeActivity";
        if (isNwd()) {
            String cur = getprop(NWD_PROP);
            if (!cur.isEmpty() && !cur.equals(me)) prefs(a).edit().putString("nwdPrev", cur).apply();
            cmd = "setprop " + NWD_PROP + " " + me + "; " + cmd;
        }
        LocalAdb.run(a, cmd, () -> a.runOnUiThread(() -> toast(a,
                "Muncul pertanyaan \"Allow USB debugging?\": centang \"Always allow\", lalu ketuk OK.")),
                (r, out) -> a.runOnUiThread(() -> {
                    done.run();
                    if (isDefault(a)) {
                        toast(a, "GoldenDash sekarang tampilan utama");
                    } else if (!isNwd() && r != LocalAdb.DONE) {
                        // Headunit biasa tanpa ADB: pilihan home Android sudah cukup.
                        openHomeSettings(a);
                    } else {
                        fail(a, failure(a, r));
                    }
                }));
    }

    /** Kembali ke launcher bawaan; GoldenDash tetap terpasang. */
    static void restore(HomeActivity a, Runnable done) {
        if (!isNwd()) {
            openHomeSettings(a);
            return;
        }
        new AlertDialog.Builder(a, Ui.DIALOG)
                .setMessage("Kembali ke launcher bawaan? GoldenDash tetap terpasang: buka dari daftar aplikasi"
                        + " untuk menjadikannya tampilan utama lagi.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Kembali", (d, w) -> {
                    String prev = prefs(a).getString("nwdPrev", "com.android.launcher");
                    ComponentName stock = stockHome(a, prev);
                    String cmd = "setprop " + NWD_PROP + " " + prev
                            + (stock != null ? "; cmd package set-home-activity " + stock.flattenToShortString() : "")
                            + "; input keyevent KEYCODE_HOME";
                    LocalAdb.run(a, cmd, null, (r, out) -> a.runOnUiThread(() -> {
                        done.run();
                        if (r != LocalAdb.DONE) fail(a, failure(a, r));
                    }));
                })
                .show();
    }

    private static String failure(Context c, int r) {
        if (r == LocalAdb.NOT_ALLOWED) {
            return "Belum diizinkan. Ketuk tombolnya lagi, centang \"Always allow\" pada pertanyaan"
                    + " \"Allow USB debugging?\", lalu ketuk OK.";
        }
        if (r == LocalAdb.NO_ADB) {
            if (!LocalAdb.debuggingOn(c)) {
                return "ADB headunit belum aktif. Buka Pengaturan Android › Tentang perangkat, ketuk \"Nomor build\""
                        + " 7 kali, lalu di Opsi pengembang nyalakan USB debugging. Setelah itu ketuk tombolnya lagi.";
            }
            return "USB debugging sudah aktif, tapi headunit ini belum membuka ADB untuk aplikasi (port 5555). Buka"
                    + " sekali dari komputer dengan: adb tcpip 5555, lalu ketuk tombolnya lagi.";
        }
        return "Headunit ini menolak mengganti tampilan utama lewat ADB.";
    }

    private static void fail(HomeActivity a, String msg) {
        new AlertDialog.Builder(a, Ui.DIALOG)
                .setTitle("Belum berhasil")
                .setMessage(msg)
                .setNegativeButton("Tutup", null)
                .setPositiveButton("Opsi pengembang", (d, w) ->
                        Ui.start(a, new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)))
                .show();
    }

    private static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }

    /** Activity HOME bawaan: milik paket {@code pkg}, atau launcher lain selain GoldenDash. */
    private static ComponentName stockHome(Context c, String pkg) {
        ComponentName other = null;
        for (ResolveInfo r : c.getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
            ComponentName cn = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            if (cn.getPackageName().equals(pkg)) return cn;
            if (other == null && !cn.getPackageName().equals(c.getPackageName())) other = cn;
        }
        return other;
    }

    static void openHomeSettings(Context c) {
        if (!Ui.start(c, new Intent(Settings.ACTION_HOME_SETTINGS))) Ui.start(c, new Intent(Settings.ACTION_SETTINGS));
    }
}
