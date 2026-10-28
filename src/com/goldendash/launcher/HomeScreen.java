package com.goldendash.launcher;

import android.app.Activity;
import android.widget.FrameLayout;

/** Layar Home: tata letak kartu sesuai mockup (kolom kiri 410, kolom kanan 704, dalam satuan desain). */
final class HomeScreen extends FrameLayout {
    HomeScreen(Activity a) {
        super(a);
        Ui.add(this, new ClockCard(a), 128, 36, 410, 270);
        Ui.add(this, new DriveCard(a), 552, 36, 704, 270);
        Ui.add(this, new ShortcutsCard(a), 128, 320, 410, 372);
        Ui.add(this, new MediaCard(a), 552, 320, 704, 272);
        Ui.add(this, SliderCard.volume(a), 552, 606, 345, 86);
        Ui.add(this, SliderCard.brightness(a), 911, 606, 345, 86);
    }
}
