package com.goldendash.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

/** Tombol sebelumnya / putar-jeda / berikutnya, dipakai kartu pemutar dan pemutar mini. */
final class Controls extends FrameLayout {
    private final ImageView play;

    /** Lebar total dalam satuan desain. */
    static float width(float playSize, float iconSize, float gap) {
        return iconSize * 2 + playSize + gap * 2;
    }

    Controls(Context c, float playSize, float iconSize, float gap) {
        super(c);
        float y = (playSize - iconSize) / 2f;
        ImageView prev = Ui.add(this, Ui.icon(c, R.drawable.ic_prev, Ui.TEXT), 0, y, iconSize, iconSize);
        Ui.pressable(prev, -1);
        prev.setOnClickListener(v -> Media.get(c).prev());

        FrameLayout pb = Ui.add(this, new FrameLayout(c), iconSize + gap, 0, playSize, playSize);
        GradientDrawable bg = Ui.glass(-1, Ui.BUTTON_T, Ui.BUTTON_B);
        pb.setBackground(bg);
        float is = playSize * 0.46f;
        play = Ui.add(pb, Ui.icon(c, R.drawable.ic_play, Ui.TEXT), (playSize - is) / 2f, (playSize - is) / 2f, is, is);
        Ui.pressable(pb, -1);
        pb.setOnClickListener(v -> {
            Media m = Media.get(c);
            if (m.has()) {
                m.playPause();
            } else if (!Media.openPlayer(c)) {
                Toast.makeText(c, "Putar musik dari aplikasi pemutar dulu", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView next = Ui.add(this, Ui.icon(c, R.drawable.ic_next, Ui.TEXT), iconSize + gap * 2 + playSize, y, iconSize, iconSize);
        Ui.pressable(next, -1);
        next.setOnClickListener(v -> Media.get(c).next());
    }

    void setPlaying(boolean p) {
        play.setImageResource(p ? R.drawable.ic_pause : R.drawable.ic_play);
    }
}
