package com.goldendash.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Pemutar mini di bawah layar Semua Aplikasi. */
final class MiniPlayer extends FrameLayout implements Media.Listener {
    private final ImageView art;
    private final TextView title, artist;
    private final Controls controls;

    MiniPlayer(Context c) {
        super(c);
        setBackground(Ui.card());
        art = Ui.add(this, new ImageView(c), 14, 10, 70, 70);
        Ui.clip(art, 12);
        title = Ui.add(this, Ui.text(c, 22, Ui.bold, Ui.TEXT), 102, 16, 470, 30);
        artist = Ui.add(this, Ui.text(c, 17, Ui.regular, Ui.TEXT2), 102, 52, 470, 24);
        float cw = Controls.width(62, 38, 50);
        controls = Ui.add(this, new Controls(c, 62, 38, 50), 600, 14, cw, 62);
        View sep = new View(c);
        sep.setBackgroundColor(Ui.LINE);
        Ui.add(this, sep, 930, 20, 1.5f, 50);
        setOnClickListener(v -> {
            Media m = Media.get(c);
            Apps.App a = m.has() ? Apps.get(c).find(m.pkg()) : null;
            if (a != null) Apps.get(c).launch(c, a);
            else Apps.get(c).launchAny(c, Sidebar.MUSIC);
        });
        show();
    }

    private void show() {
        Media m = Media.get(getContext());
        Bitmap b = m.has() ? m.art() : null;
        if (b != null) {
            art.setImageTintList(null);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setPadding(0, 0, 0, 0);
            art.setBackground(null);
            art.setImageBitmap(b);
        } else {
            art.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, Ui.dark ? new int[] {0xFF5A5DA8, 0xFF25263F} : new int[] {0xFFF0D3A8, 0xFFC4936A}));
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = Ui.px(18);
            art.setPadding(pad, pad, pad, pad);
            art.setImageResource(R.drawable.ic_music);
            art.setImageTintList(ColorStateList.valueOf(0xE6FFFFFF));
        }
        String t = m.has() ? m.title() : null, a = m.has() ? m.artist() : null;
        title.setText(t != null ? t : "Tidak ada yang diputar");
        artist.setText(a != null ? a : m.has() ? "" : "Ketuk ▶ untuk membuka pemutar musik");
        controls.setPlaying(m.playing());
    }

    @Override public void onMediaChanged() {
        show();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Media.get(getContext()).add(this);
        show();
    }

    @Override protected void onDetachedFromWindow() {
        Media.get(getContext()).remove(this);
        super.onDetachedFromWindow();
    }
}
