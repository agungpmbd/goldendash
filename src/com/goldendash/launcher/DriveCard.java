package com.goldendash.launcher;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Kartu kanan atas: speedometer GPS (bisa disembunyikan di Pengaturan) dan panel navigasi. Panel menampilkan peta langsung (LiveMap) di posisi GPS, dan
 * petunjuk belokan dari notifikasi navigasi Google Maps/Waze di atasnya. Ketuk untuk membuka aplikasi navigasinya.
 */
final class DriveCard extends FrameLayout implements Loc.Listener, Nav.Listener {
    private final SpeedGauge gauge;
    private final FrameLayout turnBox;
    private final ImageView turnIcon;
    private final TextView turnBig, turnSmall, footText;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean visible;

    /** Fix GPS bisa berhenti datang (terowongan, parkir basement): turunkan ke 0 tanpa menunggu fix baru. */
    private final Runnable stale = new Runnable() {
        @Override public void run() {
            if (gauge != null) gauge.setSpeed(Loc.get(getContext()).speedKmh());
            if (visible && gauge != null) h.postDelayed(this, 2000);
        }
    };

    DriveCard(Context c) {
        super(c);
        setBackground(Ui.card());
        // Tanpa speedometer, peta mengambil seluruh lebar kartu.
        float mapX = 184;
        if (Ui.showSpeed(c)) {
            FrameLayout box = Ui.add(this, new FrameLayout(c), 10, 10, 164, 250);
            box.setBackground(Ui.inner());
            gauge = Ui.add(box, new SpeedGauge(c), 0, 0, 164, 250);
        } else {
            gauge = null;
            mapX = 10;
        }
        float mapW = 694 - mapX;

        FrameLayout map = Ui.add(this, new FrameLayout(c), mapX, 10, mapW, 250);
        Ui.clip(map, 20);
        map.addView(new MapDecor(c), new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Peta langsung menutupi peta hiasan begitu ada fix GPS.
        map.addView(new LiveMap(c), new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        turnBox = Ui.add(map, new FrameLayout(c), 12, 12, 300, 104);
        turnBox.setBackground(Ui.over(18));
        turnIcon = Ui.add(turnBox, new ImageView(c), 14, 19, 66, 66);
        turnIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        turnBig = Ui.add(turnBox, Ui.text(c, 30, Ui.bold, Ui.TEXT), 92, 14, 196, 36);
        turnSmall = Ui.add(turnBox, Ui.text(c, 17, Ui.medium, Ui.TEXT), 92, 54, 196, 42);
        turnSmall.setSingleLine(false);
        turnSmall.setMaxLines(2);

        FrameLayout footer = Ui.add(map, new FrameLayout(c), 12, 196, mapW - 24, 42);
        footer.setBackground(Ui.over(21));
        Ui.add(footer, Ui.icon(c, R.drawable.ic_pin, Ui.TEXT), 14, 10, 22, 22);
        footText = Ui.add(footer, Ui.text(c, 17, Ui.medium, Ui.TEXT), 44, 0, mapW - 80, 42);
        footText.setGravity(Gravity.CENTER_VERTICAL);

        Ui.pressable(map, 20);
        map.setOnClickListener(v -> openNav());
        showNav();
    }

    private void showNav() {
        if (Nav.active) {
            turnBox.setVisibility(View.VISIBLE);
            if (Nav.icon != null) {
                turnIcon.setImageBitmap(Nav.icon);
            } else {
                turnIcon.setImageResource(R.drawable.ic_turn);
            }
            // Ikon belokan dari Maps/Waze diwarnai biru supaya serasi dengan tema.
            turnIcon.setImageTintList(ColorStateList.valueOf(Ui.ACCENT));
            turnBig.setText(Nav.primary);
            turnSmall.setText(Nav.secondary != null ? Nav.secondary : "");
            footText.setText(Nav.footer != null ? Nav.footer : "Navigasi aktif · ketuk untuk membuka");
        } else {
            turnBox.setVisibility(View.GONE);
            footText.setText(Media.allowed(getContext())
                    ? "Navigasi tidak aktif · ketuk untuk membuka Maps"
                    : "Izinkan akses notifikasi untuk petunjuk belokan");
        }
    }

    private void openNav() {
        Context c = getContext();
        if (Nav.active && Nav.open != null) {
            try {
                Nav.open.send();
                return;
            } catch (PendingIntent.CanceledException ignored) { }
        }
        if (!Apps.get(c).launchAny(c, Sidebar.MAPS))
            Toast.makeText(c, "Google Maps atau Waze belum terpasang", Toast.LENGTH_SHORT).show();
    }

    @Override public void onLocation(Location l) {
        if (gauge != null) gauge.setSpeed(Loc.get(getContext()).speedKmh());
    }

    @Override public void onNavChanged() {
        showNav();
    }

    @Override public void onVisibilityAggregated(boolean v) {
        super.onVisibilityAggregated(v);
        visible = v;
        h.removeCallbacks(stale);
        if (v) {
            h.post(stale);
            showNav();
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Loc.get(getContext()).add(this);
        Nav.add(this);
        showNav();
    }

    @Override protected void onDetachedFromWindow() {
        Loc.get(getContext()).remove(this);
        Nav.remove(this);
        h.removeCallbacks(stale);
        super.onDetachedFromWindow();
    }
}
