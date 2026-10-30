package com.goldendash.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    private static final String[] SIDES = {"Kiri", "Kanan"};
    private static final String[] SPEEDS = {"Tampilkan", "Sembunyikan"};
    private static final float ROW_H = 66, BTN_Y = 8, BTN_H = 50;

    private final HomeActivity act;
    private final TextView launcherBtn, locBtn, notifBtn, brightBtn;
    private final TextView[] themeBtns = new TextView[THEMES.length];
    private final TextView[] sideBtns = new TextView[SIDES.length];
    private final TextView[] speedBtns = new TextView[SPEEDS.length];

    SettingsScreen(HomeActivity a) {
        super(a);
        act = a;
        TextView title = Ui.add(this, Ui.text(a, 48, Ui.bold, Ui.TEXT), 140, 30, 600, 60);
        title.setText("Pengaturan");

        float y = 96, step = 74;
        FrameLayout theme = rowBox(y, "Tema", "Terang keemasan, gelap dengan wallpaper, atau otomatis (gelap pukul 18.00–06.00).", 700);
        for (int i = 0; i < THEMES.length; i++) {
            final String t = THEMES[i];
            themeBtns[i] = choice(theme, i, 3, THEME_NAMES[i], v -> act.changeTheme(t));
        }
        FrameLayout side = rowBox(y += step, "Posisi menu", "Letak bilah menu: kiri atau kanan layar.", 700);
        for (int i = 0; i < SIDES.length; i++) {
            final boolean right = i == 1;
            sideBtns[i] = choice(side, i, 2, SIDES[i], v -> act.changeSidebar(right));
        }
        FrameLayout speed = rowBox(y += step, "Speedometer", "Sembunyikan bila mobil sudah punya speedometer; peta melebar memenuhi kartu.", 700);
        for (int i = 0; i < SPEEDS.length; i++) {
            final boolean show = i == 0;
            speedBtns[i] = choice(speed, i, 2, SPEEDS[i], v -> act.changeSpeed(show));
        }
        launcherBtn = row(y += step, "Jadikan tampilan utama",
                "Tetap tampil setiap headunit menyala. Di firmware NWD perlu USB debugging aktif (tanpa komputer).",
                v -> {
                    if (HomeSwitch.isDefault(act)) HomeSwitch.restore(act, this::refresh);
                    else HomeSwitch.makeDefault(act, this::refresh);
                });
        locBtn = row(y += step, "Lokasi (GPS)", "Untuk speedometer, peta, dan cuaca.", v -> act.askLocation());
        notifBtn = row(y += step, "Akses notifikasi",
                "Untuk pemutar musik dan petunjuk belokan Maps/Waze di home. Notifikasi aplikasi lain tidak dibaca.",
                v -> Ui.start(act, new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        brightBtn = row(y += step, "Ubah kecerahan layar",
                "Supaya slider Kecerahan berlaku untuk seluruh sistem, bukan hanya di home.",
                v -> Ui.start(act, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + act.getPackageName()))));
        TextView sys = row(y += step, "Pengaturan Android", "Buka pengaturan sistem headunit.",
                v -> Ui.start(act, new Intent(Settings.ACTION_SETTINGS)));
        state(sys, false, "", "Buka");

        TextView ver = Ui.add(this, Ui.text(a, 15, Ui.regular, Ui.TEXT3), 140, 692, 600, 20);
        ver.setText("GoldenDash " + version());
        refresh();
    }

    private FrameLayout rowBox(float y, String title, String desc, float descW) {
        Context c = getContext();
        FrameLayout r = Ui.add(this, new FrameLayout(c), 128, y, 1128, ROW_H);
        r.setBackground(Ui.card());
        TextView t = Ui.add(r, Ui.text(c, 21, Ui.medium, Ui.TEXT), 28, 7, 700, 28);
        t.setText(title);
        TextView d = Ui.add(r, Ui.text(c, 16, Ui.regular, Ui.TEXT2), 28, 37, descW, 22);
        d.setText(desc);
        return r;
    }

    /** Tombol pilihan ke-i dari n, rata kanan dalam baris (lebar total sama dengan tombol aksi tunggal ±352). */
    private TextView choice(FrameLayout row, int i, int n, String label, View.OnClickListener click) {
        float w = n == 2 ? 172 : 112, gap = 8, start = 1100 - n * w - (n - 1) * gap;
        TextView b = Ui.add(row, Ui.text(getContext(), 18, Ui.medium, Ui.TEXT), start + i * (w + gap), BTN_Y, w, BTN_H);
        b.setGravity(Gravity.CENTER);
        b.setText(label);
        Ui.pressable(b, 26);
        b.setOnClickListener(click);
        return b;
    }

    private TextView row(float y, String title, String desc, View.OnClickListener click) {
        FrameLayout r = rowBox(y, title, desc, 850);
        TextView b = Ui.add(r, Ui.text(getContext(), 18, Ui.medium, Ui.WHITE), 900, BTN_Y, 200, BTN_H);
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
        boolean right = Ui.sidebarRight(act);
        for (int i = 0; i < SIDES.length; i++) {
            boolean on = (i == 1) == right;
            sideBtns[i].setBackground(on ? Ui.accent(26) : Ui.field(26));
            sideBtns[i].setTextColor(on ? Ui.WHITE : Ui.TEXT2);
        }
        boolean show = Ui.showSpeed(act);
        for (int i = 0; i < SPEEDS.length; i++) {
            boolean on = (i == 0) == show;
            speedBtns[i].setBackground(on ? Ui.accent(26) : Ui.field(26));
            speedBtns[i].setTextColor(on ? Ui.WHITE : Ui.TEXT2);
        }
        state(launcherBtn, HomeSwitch.isDefault(act), "Aktif", "Jadikan utama");
        state(locBtn, Loc.allowed(act), "Diizinkan", "Izinkan");
        state(notifBtn, Media.allowed(act), "Diizinkan", "Buka");
        state(brightBtn, Settings.System.canWrite(act), "Diizinkan", "Izinkan");
    }

    private String version() {
        try {
            return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
