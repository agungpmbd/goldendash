package com.goldendash.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Format jam dan tanggal berbahasa Indonesia, dan siaran yang menandai jam perlu digambar ulang. */
final class Clock {
    private static final Locale ID = new Locale("id", "ID");

    private Clock() { }

    static String time(Context c, Date d) {
        return new SimpleDateFormat(DateFormat.is24HourFormat(c) ? "HH:mm" : "h:mm", ID).format(d);
    }

    static String date(Date d) {
        return new SimpleDateFormat("EEEE, d MMMM", ID).format(d);
    }

    static IntentFilter filter() {
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        return f;
    }
}
