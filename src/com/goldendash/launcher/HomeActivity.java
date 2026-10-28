package com.goldendash.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Activity HOME satu-satunya. Tiga layar (Home, Semua Aplikasi, Pengaturan) dan sidebar hidup di satu "panggung"
 * 1280×720 yang diskalakan ke layar; berpindah layar cukup mengganti visibilitas, tanpa membuka Activity baru.
 */
public final class HomeActivity extends Activity implements Sidebar.Nav {
    static final int REQ_LOC = 1;

    private FrameLayout root, stage;
    private SilkBackground backdrop;
    private Sidebar sidebar;
    private View home;
    private AppsScreen apps;
    private SettingsScreen settings;
    private int screen = Sidebar.HOME, builtW, builtH;

    /** Tema Otomatis: cek tiap menit apakah sudah waktunya berganti terang/gelap. */
    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            applyTheme();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Ui.theme(Ui.wantDark(this));
        windowColors();
        root = new FrameLayout(this) {
            @Override protected void onSizeChanged(final int w, final int h, int ow, int oh) {
                super.onSizeChanged(w, h, ow, oh);
                if (w > 0 && h > 0) post(() -> build(w, h));
            }
        };
        backdrop = new SilkBackground(this);
        root.addView(backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        registerReceiver(tick, Clock.filter());
        if (!Loc.allowed(this)) askLocation();
    }

    @Override protected void onDestroy() {
        unregisterReceiver(tick);
        super.onDestroy();
    }

    /** Pilihan tema dari Pengaturan (Ui.LIGHT / DARK / AUTO). */
    void changeTheme(String t) {
        Ui.setThemePref(this, t);
        applyTheme();
        if (settings != null) settings.refresh();
    }

    /** Ganti ke tema yang seharusnya aktif, bila berbeda: gambar ulang latar dan bangun ulang semua layar. */
    void applyTheme() {
        boolean d = Ui.wantDark(this);
        if (d == Ui.dark) return;
        Ui.theme(d);
        windowColors();
        backdrop.refresh();
        if (stage != null) {
            builtW = builtH = 0;
            build(root.getWidth(), root.getHeight());
        }
    }

    private void windowColors() {
        getWindow().setBackgroundDrawable(new ColorDrawable(Ui.WINDOW));
        getWindow().setNavigationBarColor(Ui.WINDOW);
    }

    void askLocation() {
        requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
    }

    /** Bangun ulang seluruh tampilan untuk ukuran layar ini (sekali saat mulai, lagi hanya bila ukurannya berubah). */
    private void build(int w, int h) {
        if (w == builtW && h == builtH) return;
        builtW = w;
        builtH = h;
        Ui.init(w, h);
        if (stage != null) root.removeView(stage);
        stage = new FrameLayout(this);
        root.addView(stage, new FrameLayout.LayoutParams(Ui.px(Ui.W), Ui.px(Ui.H), Gravity.CENTER));
        home = Ui.add(stage, new HomeScreen(this), 0, 0, Ui.W, Ui.H);
        apps = Ui.add(stage, new AppsScreen(this, this), 0, 0, Ui.W, Ui.H);
        settings = Ui.add(stage, new SettingsScreen(this), 0, 0, Ui.W, Ui.H);
        sidebar = Ui.add(stage, new Sidebar(this, this), 0, 0, 108, Ui.H);
        go(screen);
    }

    @Override public void go(int s) {
        if (stage == null) {
            screen = s;
            return;
        }
        if (s != Sidebar.APPS) apps.hideKeyboard();
        screen = s;
        home.setVisibility(s == Sidebar.HOME ? View.VISIBLE : View.GONE);
        apps.setVisibility(s == Sidebar.APPS ? View.VISIBLE : View.GONE);
        settings.setVisibility(s == Sidebar.SETTINGS ? View.VISIBLE : View.GONE);
        if (s == Sidebar.SETTINGS) settings.refresh();
        sidebar.select(s);
    }

    @Override protected void onResume() {
        super.onResume();
        applyTheme();
        Loc.get(this).start();
        Media.get(this).start();
        if (settings != null) settings.refresh();
    }

    @Override protected void onPause() {
        Loc.get(this).stop();
        super.onPause();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        // Tombol HOME ditekan saat launcher sudah di depan: kembali ke layar Home.
        if (Intent.ACTION_MAIN.equals(i.getAction()) && i.hasCategory(Intent.CATEGORY_HOME)) go(Sidebar.HOME);
    }

    @Override public void onBackPressed() {
        if (screen != Sidebar.HOME) go(Sidebar.HOME);
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        Loc.get(this).start();
        if (settings != null) settings.refresh();
    }
}
