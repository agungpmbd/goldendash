package com.goldendash.launcher;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

/** Bilah kiri: jam kecil, status WiFi/Bluetooth, dan navigasi Home, Maps, musik, Semua Aplikasi, Pengaturan. */
final class Sidebar extends FrameLayout {
    interface Nav {
        void go(int screen);
    }

    static final int HOME = 0, APPS = 1, SETTINGS = 2;
    static final String[] MAPS = {"com.google.android.apps.maps", "com.waze"};
    static final String[] MUSIC = {"com.spotify.music", "com.google.android.apps.youtube.music",
        "com.tencent.ibg.joox", "com.apple.android.music"};

    private final TextView clock;
    private final ImageView wifi, bt;
    private final Item home, maps, music, apps, settings;

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            refresh();
        }
    };
    private final Apps.Listener appsListener = this::loadApps;
    /** Tombol Musik mengikuti aplikasi yang terakhir memutar musik. */
    private final Media.Listener mediaListener = this::loadApps;

    Sidebar(Context c, final Nav nav, boolean right) {
        super(c);
        // Sisi yang menempel ke tepi layar lebih pekat; garis tipis di sisi yang menghadap isi.
        setBackground(new GradientDrawable(right ? GradientDrawable.Orientation.RIGHT_LEFT : GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Ui.SIDE_A, Ui.SIDE_B}));
        View edge = new View(c);
        edge.setBackgroundColor(Ui.STROKE);
        addView(edge, new LayoutParams(Math.max(1, Ui.px(1.5f)), ViewGroup.LayoutParams.MATCH_PARENT,
                right ? Gravity.START : Gravity.END));

        clock = Ui.add(this, Ui.text(c, 30, Ui.medium, Ui.TEXT), 0, 22, 108, 38);
        clock.setGravity(Gravity.CENTER_HORIZONTAL);
        wifi = Ui.add(this, Ui.icon(c, R.drawable.ic_wifi, Ui.TEXT), 29, 70, 22, 22);
        bt = Ui.add(this, Ui.icon(c, R.drawable.ic_bluetooth, Ui.TEXT), 57, 70, 22, 22);

        home = item(c, 112, R.drawable.ic_home, "Home");
        home.setOnClickListener(v -> nav.go(HOME));
        maps = item(c, 222, R.drawable.ic_pin, "Maps");
        maps.setOnClickListener(v -> launch(MAPS));
        music = item(c, 332, R.drawable.ic_music, "Musik");
        music.setOnClickListener(v -> {
            if (!Media.openPlayer(getContext()))
                Toast.makeText(getContext(), "Aplikasi pemutar musik belum terpasang", Toast.LENGTH_SHORT).show();
        });
        apps = item(c, 442, R.drawable.ic_grid, "Semua\nAplikasi");
        apps.setOnClickListener(v -> nav.go(APPS));

        View line = new View(c);
        line.setBackgroundColor(Ui.LINE);
        Ui.add(this, line, 24, 562, 60, 1.5f);

        settings = item(c, 584, R.drawable.ic_gear, "Pengaturan");
        settings.setOnClickListener(v -> nav.go(SETTINGS));
        select(HOME);
    }

    private Item item(Context c, float y, int icon, String label) {
        return Ui.add(this, new Item(c, icon, label), 10, y, 88, 100);
    }

    void select(int screen) {
        home.select(screen == HOME);
        maps.select(false);
        music.select(false);
        apps.select(screen == APPS);
        settings.select(screen == SETTINGS);
    }

    private void launch(String[] pkgs) {
        if (!Apps.get(getContext()).launchAny(getContext(), pkgs))
            Toast.makeText(getContext(), "Aplikasi belum terpasang", Toast.LENGTH_SHORT).show();
    }

    private void loadApps() {
        Apps a = Apps.get(getContext());
        Apps.App m = a.find(MAPS);
        if (m != null) maps.app(m.icon, "Maps");
        Apps.App p = Media.player(getContext());
        if (p != null) music.app(p.icon, p.label);
    }

    @SuppressWarnings("deprecation")
    private void refresh() {
        Context c = getContext();
        clock.setText(Clock.time(c, new Date()));
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        boolean wifiOn = ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
        wifi.setImageTintList(ColorStateList.valueOf(wifiOn ? Ui.TEXT : Ui.OFF));
        boolean btOn = false;
        try {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            btOn = ba != null && ba.isEnabled();
        } catch (RuntimeException ignored) { }
        bt.setImageTintList(ColorStateList.valueOf(btOn ? Ui.TEXT : Ui.OFF));
    }

    @SuppressWarnings("deprecation")
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter f = Clock.filter();
        f.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(rx, f);
        Apps.get(getContext()).add(appsListener);
        Media.get(getContext()).add(mediaListener);
        refresh();
        loadApps();
    }

    @Override protected void onDetachedFromWindow() {
        getContext().unregisterReceiver(rx);
        Apps.get(getContext()).remove(appsListener);
        Media.get(getContext()).remove(mediaListener);
        super.onDetachedFromWindow();
    }

    /** Satu tombol sidebar: ikon di atas, label di bawah; yang aktif berlatar biru muda. */
    private static final class Item extends FrameLayout {
        private final FrameLayout box;
        private final ImageView icon;
        private final TextView label;
        private boolean hasApp;

        Item(Context c, int res, String text) {
            super(c);
            box = Ui.add(this, new FrameLayout(c), 18, 8, 52, 52);
            icon = Ui.add(box, Ui.icon(c, res, Ui.TEXT2), 8, 8, 36, 36);
            label = Ui.text(c, 14, Ui.medium, Ui.TEXT2);
            label.setSingleLine(false);
            label.setMaxLines(2);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setText(text);
            Ui.add(this, label, 0, 66, 88, 34);
            Ui.pressable(this, 22);
        }

        void app(Bitmap b, String text) {
            if (b == null) return;
            hasApp = true;
            icon.setImageTintList(null);
            icon.setImageBitmap(b);
            icon.setLayoutParams(Ui.at(2, 2, 48, 48));
            box.setBackground(null);
            label.setText(text);
        }

        void select(boolean on) {
            setBackground(on ? Ui.glass(22, Ui.SELECT_T, Ui.SELECT_B) : null);
            if (!hasApp) {
                box.setBackground(on ? null : Ui.round(Ui.CHIP, 16));
                icon.setImageTintList(ColorStateList.valueOf(on ? Ui.SELECT_FG : Ui.TEXT2));
            }
            label.setTextColor(on ? Ui.SELECT_FG : Ui.TEXT2);
        }
    }
}
