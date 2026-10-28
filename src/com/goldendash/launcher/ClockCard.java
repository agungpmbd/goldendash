package com.goldendash.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;

/** Kartu kiri atas: jam besar, tanggal, dan cuaca di lokasi sekarang. */
final class ClockCard extends FrameLayout implements Loc.Listener {
    private final TextView time, date, city, temp, cond;
    private final WeatherIcon icon;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            showTime();
        }
    };

    ClockCard(Context c) {
        super(c);
        setBackground(Ui.card());
        time = Ui.add(this, Ui.text(c, 100, Ui.bold, Ui.TEXT), 28, 18, 370, 108);
        time.setLetterSpacing(-0.03f);
        date = Ui.add(this, Ui.text(c, 23, Ui.medium, Ui.TEXT), 32, 126, 360, 30);
        icon = Ui.add(this, new WeatherIcon(c), 24, 166, 116, 90);
        city = Ui.add(this, Ui.text(c, 18, Ui.medium, Ui.TEXT), 158, 170, 230, 24);
        temp = Ui.add(this, Ui.text(c, 46, Ui.bold, Ui.TEXT), 156, 194, 230, 52);
        cond = Ui.add(this, Ui.text(c, 18, Ui.regular, Ui.TEXT), 158, 240, 230, 24);
        Weather.load(c);
        showTime();
        showWeather();
    }

    private void showTime() {
        Date now = new Date();
        time.setText(Clock.time(getContext(), now));
        date.setText(Clock.date(now));
    }

    private void showWeather() {
        boolean has = Weather.temp != Weather.NO_TEMP;
        city.setText(Weather.city != null ? Weather.city : "Cuaca");
        temp.setText(has ? Weather.temp + "°" : "--°");
        String d = Weather.desc();
        cond.setText(d != null ? d : Loc.allowed(getContext()) ? "Menunggu lokasi GPS" : "Izinkan lokasi di Pengaturan");
        icon.invalidate();
    }

    @Override public void onLocation(Location l) {
        Weather.update(getContext(), l, this::showWeather);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().registerReceiver(tick, Clock.filter());
        Loc.get(getContext()).add(this);
        showTime();
    }

    @Override protected void onDetachedFromWindow() {
        getContext().unregisterReceiver(tick);
        Loc.get(getContext()).remove(this);
        super.onDetachedFromWindow();
    }
}
