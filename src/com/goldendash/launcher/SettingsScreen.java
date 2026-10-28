package com.goldendash.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Layar Pengaturan: izin yang dibutuhkan fitur home, beserta statusnya. */
final class SettingsScreen extends FrameLayout {
    private static final String[] THEMES = {Ui.LIGHT, Ui.DARK, Ui.AUTO};
    private static final String[] THEME_NAMES = {"Terang", "Gelap", "Otomatis"};

    private final HomeActivity act;
    private final TextView launcherBtn, locBtn, notifBtn, brightBtn;
    private final TextView[] themeBtns = new TextView[THEMES.length];

    SettingsScreen(HomeActivity a) {
        super(a);
        act = a;
        TextView title = Ui.add(this, Ui.text(a, 52, Ui.bold, Ui.TEXT), 140, 40, 600, 68);
        title.setText("Pengaturan");

        float y = 124, step = 90;
        FrameLayout theme = rowBox(y, "Tema", "Terang keemasan, gelap dengan wallpaper, atau otomatis (gelap pukul 18.00–06.00).", 700);
        for (int i = 0; i < THEMES.length; i++) {
            final String t = THEMES[i];
            TextView b = Ui.add(theme, Ui.text(a, 18, Ui.medium, Ui.TEXT), 748 + i * 120, 14, 112, 52);
            b.setGravity(Gravity.CENTER);
            b.setText(THEME_NAMES[i]);
            Ui.pressable(b, 26);
            b.setOnClickListener(v -> act.changeTheme(t));
            themeBtns[i] = b;
        }
        launcherBtn = row(y += step, "Jadikan tampilan utama",
                "Pakai GoldenDash sebagai Home. Di firmware NWD, jalankan juga ./build.sh install dari komputer.",
                v -> openHomeSettings());
        locBtn = row(y += step, "Lokasi (GPS)", "Untuk speedometer dan cuaca.", v -> act.askLocation());
        notifBtn = row(y += step, "Akses notifikasi",
                "Untuk pemutar musik dan petunjuk belokan Maps/Waze di home. Notifikasi aplikasi lain tidak dibaca.",
                v -> Ui.start(act, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        brightBtn = row(y += step, "Ubah kecerahan layar",
                "Supaya slider Kecerahan berlaku untuk seluruh sistem, bukan hanya di home.",
                v -> Ui.start(act, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + act.getPackageName()))));
        TextView sys = row(y += step, "Pengaturan Android", "Buka pengaturan sistem headunit.",
                v -> Ui.start(act, new Intent(Settings.ACTION_SETTINGS)));
        state(sys, false, "", "Buka");

        TextView ver = Ui.add(this, Ui.text(a, 15, Ui.regular, Ui.TEXT3), 140, 672, 600, 20);
        ver.setText("GoldenDash " + version());
        refresh();
    }

    private FrameLayout rowBox(float y, String title, String desc, float descW) {
        Context c = getContext();
        FrameLayout r = Ui.add(this, new FrameLayout(c), 128, y, 1128, 80);
        r.setBackground(Ui.card());
        TextView t = Ui.add(r, Ui.text(c, 21, Ui.medium, Ui.TEXT), 28, 12, 700, 28);
        t.setText(title);
        TextView d = Ui.add(r, Ui.text(c, 16, Ui.regular, Ui.TEXT2), 28, 46, descW, 22);
        d.setText(desc);
        return r;
    }

    private TextView row(float y, String title, String desc, View.OnClickListener click) {
        FrameLayout r = rowBox(y, title, desc, 850);
        TextView b = Ui.add(r, Ui.text(getContext(), 18, Ui.medium, Ui.WHITE), 900, 14, 200, 52);
        b.setGravity(Gravity.CENTER);
        Ui.pressable(b, 26);
        b.setOnClickListener(click);
        return b;
    }

    private static void state(TextView b, boolean ok, String on, String off) {
        b.setText(ok ? on : off);
        b.setBackground(ok ? Ui.field(26) : Ui.accent(26));
        b.setTextColor(ok ? Ui.TEXT2 : Ui.WHITE);
    }

    void refresh() {
        String cur = Ui.themePref(act);
        for (int i = 0; i < THEMES.length; i++) {
            boolean on = THEMES[i].equals(cur);
            themeBtns[i].setBackground(on ? Ui.accent(26) : Ui.field(26));
            themeBtns[i].setTextColor(on ? Ui.WHITE : Ui.TEXT2);
        }
        state(launcherBtn, isDefaultHome(), "Aktif", "Atur");
        state(locBtn, Loc.allowed(act), "Diizinkan", "Izinkan");
        state(notifBtn, Media.allowed(act), "Diizinkan", "Buka");
        state(brightBtn, Settings.System.canWrite(act), "Diizinkan", "Izinkan");
    }

    private boolean isDefaultHome() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = act.getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null && ri.activityInfo != null && act.getPackageName().equals(ri.activityInfo.packageName);
    }

    private void openHomeSettings() {
        try {
            act.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (RuntimeException e) {
            Ui.start(act, new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private String version() {
        try {
            return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
