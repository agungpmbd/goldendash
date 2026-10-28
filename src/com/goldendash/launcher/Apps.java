package com.goldendash.launcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Daftar aplikasi terpasang beserta ikon, kategori (Navigasi/Media/Sistem), aplikasi tersembunyi, dan jumlah dibuka.
 * Dimuat di thread latar dan dimuat ulang otomatis saat ada aplikasi dipasang, diperbarui, atau dihapus.
 */
final class Apps {
    static final int OTHER = 0, NAV = 1, MEDIA = 2, SYSTEM = 3;

    private static final String[] NAV_WORDS = {"maps", "waze", "navi", "sygic", "osmand", "peta", "gps"};
    private static final String[] MEDIA_WORDS = {"music", "musik", "spotify", "youtube", "radio", "video", "player",
        "joox", "podcast", "media", "audio", "netflix", "tiktok"};
    private static final String[] SYSTEM_WORDS = {"settings", "pengaturan", "setelan", "file", "bluetooth"};

    static final class App {
        final ComponentName cn;
        final String label;
        final int cat;
        final Bitmap icon;

        App(ComponentName cn, String label, int cat, Bitmap icon) {
            this.cn = cn;
            this.label = label;
            this.cat = cat;
            this.icon = icon;
        }

        String key() {
            return cn.flattenToShortString();
        }

        String pkg() {
            return cn.getPackageName();
        }
    }

    interface Listener {
        void onAppsChanged();
    }

    private static Apps instance;

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private List<App> all = Collections.emptyList();
    private boolean loaded, loading, again;

    static Apps get(Context c) {
        if (instance == null) instance = new Apps(c.getApplicationContext());
        return instance;
    }

    private Apps(Context c) {
        ctx = c;
        prefs = c.getSharedPreferences("apps", Context.MODE_PRIVATE);
        BroadcastReceiver rx = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                reload();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        c.registerReceiver(rx, f);
        IntentFilter sd = new IntentFilter();
        sd.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sd.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        c.registerReceiver(rx, sd);
        reload();
    }

    void add(Listener l) {
        listeners.add(l);
    }

    void remove(Listener l) {
        listeners.remove(l);
    }

    boolean loaded() {
        return loaded;
    }

    SharedPreferences prefs() {
        return prefs;
    }

    List<App> all() {
        return all;
    }

    List<App> visible() {
        Set<String> hidden = hidden();
        List<App> out = new ArrayList<>();
        for (App a : all) if (!hidden.contains(a.key())) out.add(a);
        return out;
    }

    List<App> hiddenApps() {
        Set<String> hidden = hidden();
        List<App> out = new ArrayList<>();
        for (App a : all) if (hidden.contains(a.key())) out.add(a);
        return out;
    }

    private Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet("hidden", Collections.<String>emptySet()));
    }

    void setHidden(App a, boolean hide) {
        Set<String> h = hidden();
        if (hide) h.add(a.key());
        else h.remove(a.key());
        prefs.edit().putStringSet("hidden", h).apply();
    }

    int count(App a) {
        return prefs.getInt("n:" + a.key(), 0);
    }

    /** Aplikasi pertama dari daftar paket yang terpasang. */
    App find(String... pkgs) {
        for (String p : pkgs) {
            if (p == null) continue;
            for (App a : all) if (a.pkg().equals(p)) return a;
        }
        return null;
    }

    App byKey(String key) {
        for (App a : all) if (a.key().equals(key)) return a;
        return null;
    }

    /** Aplikasi pertama yang labelnya memuat salah satu kata (kata pendek harus berdiri sendiri). */
    App findLabel(String... words) {
        for (String w : words) {
            for (App a : all) {
                String l = " " + a.label.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ") + " ";
                if (w.length() > 3 ? l.contains(w) : l.contains(" " + w + " ")) return a;
            }
        }
        return null;
    }

    boolean launch(Context c, App a) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(a.cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (!Ui.start(c, i)) return false;
        prefs.edit().putInt("n:" + a.key(), count(a) + 1).apply();
        return true;
    }

    /** Buka aplikasi pertama yang terpasang dari daftar paket; false bila tidak ada satu pun. */
    boolean launchAny(Context c, String... pkgs) {
        App a = find(pkgs);
        return a != null && launch(c, a);
    }

    void reload() {
        if (loading) {
            again = true;
            return;
        }
        loading = true;
        final int size = Math.max(96, Ui.px(124));
        new Thread(() -> {
            final List<App> list = load(size);
            main.post(() -> {
                all = list;
                loaded = true;
                loading = false;
                for (Listener l : new ArrayList<>(listeners)) l.onAppsChanged();
                if (again) {
                    again = false;
                    reload();
                }
            });
        }, "apps").start();
    }

    private List<App> load(int size) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(i, 0);
        List<App> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : ris) {
            ActivityInfo ai = ri.activityInfo;
            if (ai == null || ai.packageName.equals(ctx.getPackageName())) continue;
            ComponentName cn = new ComponentName(ai.packageName, ai.name);
            if (!seen.add(cn.flattenToShortString())) continue;
            String label = String.valueOf(ri.loadLabel(pm)).trim();
            Bitmap icon;
            try {
                icon = render(sharpIcon(pm, ri), size);
            } catch (RuntimeException e) {
                icon = null;
            }
            out.add(new App(cn, label, categorize(ai.applicationInfo, label), icon));
        }
        final Collator col = Collator.getInstance(new Locale("id", "ID"));
        Collections.sort(out, (a, b) -> col.compare(a.label, b.label));
        return out;
    }

    /**
     * Ikon versi resolusi tertinggi: headunit 160 dpi biasanya memberi ikon 48 px yang buram bila dibesarkan ke ukuran
     * grid. Bila tidak ada, pakai ikon biasa.
     */
    private static Drawable sharpIcon(PackageManager pm, ResolveInfo ri) {
        int res = ri.getIconResource();
        if (res != 0) {
            try {
                Drawable d = pm.getResourcesForApplication(ri.activityInfo.applicationInfo)
                        .getDrawableForDensity(res, DisplayMetrics.DENSITY_XXXHIGH, null);
                if (d != null) return d;
            } catch (PackageManager.NameNotFoundException | RuntimeException ignored) { }
        }
        return ri.loadIcon(pm);
    }

    /** Ikon adaptif (Android 8+) dijadikan persegi bersudut bulat seperti di mockup; ikon lama digambar apa adanya. */
    private static Bitmap render(Drawable d, int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (Build.VERSION.SDK_INT >= 26 && d instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) d;
            Path clip = new Path();
            float r = size * 0.24f;
            clip.addRoundRect(0, 0, size, size, r, r, Path.Direction.CW);
            c.clipPath(clip);
            // Lapisan adaptif 108dp, yang terlihat hanya 72dp di tengah: lebihkan 1/4 ukuran di tiap sisi.
            int o = Math.round(size * 0.25f);
            Drawable[] layers = {a.getBackground(), a.getForeground()};
            for (Drawable l : layers) {
                if (l == null) continue;
                l.setBounds(-o, -o, size + o, size + o);
                l.draw(c);
            }
        } else {
            d.setBounds(0, 0, size, size);
            d.draw(c);
        }
        return b;
    }

    private static int categorize(ApplicationInfo ai, String label) {
        if (Build.VERSION.SDK_INT >= 26) {
            int c = ai.category;
            if (c == ApplicationInfo.CATEGORY_MAPS) return NAV;
            if (c == ApplicationInfo.CATEGORY_AUDIO || c == ApplicationInfo.CATEGORY_VIDEO) return MEDIA;
        }
        String p = ai.packageName.toLowerCase(Locale.ROOT), l = label.toLowerCase(Locale.ROOT);
        if (has(p, l, NAV_WORDS)) return NAV;
        if (has(p, l, MEDIA_WORDS)) return MEDIA;
        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 || has(p, l, SYSTEM_WORDS)) return SYSTEM;
        return OTHER;
    }

    private static boolean has(String pkg, String label, String[] words) {
        for (String w : words) if (pkg.contains(w) || label.contains(w)) return true;
        return false;
    }
}
