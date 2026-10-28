package com.goldendash.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Kartu Pintasan: 3×2 aplikasi. Ketuk pensil (atau tekan lama satu ikon) untuk mengganti isinya. */
final class ShortcutsCard extends FrameLayout implements Apps.Listener, SharedPreferences.OnSharedPreferenceChangeListener {
    private final FrameLayout[] cells = new FrameLayout[Shortcuts.N];
    private final ImageView[] icons = new ImageView[Shortcuts.N];
    private final TextView[] labels = new TextView[Shortcuts.N];
    private final TextView title;
    private final ImageView edit;
    private boolean editing;

    ShortcutsCard(Context c) {
        super(c);
        setBackground(Ui.card());
        title = Ui.add(this, Ui.text(c, 22, Ui.bold, Ui.TEXT), 26, 26, 300, 30);
        title.setText("Pintasan");
        edit = Ui.add(this, Ui.icon(c, R.drawable.ic_edit, Ui.TEXT2), 352, 20, 40, 40);
        edit.setPadding(Ui.px(7), Ui.px(7), Ui.px(7), Ui.px(7));
        Ui.pressable(edit, -1);
        edit.setOnClickListener(v -> setEditing(!editing));

        for (int i = 0; i < Shortcuts.N; i++) {
            final int slot = i;
            FrameLayout cell = Ui.add(this, new FrameLayout(c), 16 + (i % 3) * 126, 74 + (i / 3) * 150, 126, 136);
            icons[i] = Ui.add(cell, new ImageView(c), 21, 0, 84, 84);
            icons[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
            labels[i] = Ui.add(cell, Ui.text(c, 16, Ui.medium, Ui.TEXT), 0, 96, 126, 24);
            labels[i].setGravity(Gravity.CENTER_HORIZONTAL);
            Ui.pressable(cell, 20);
            cell.setOnClickListener(v -> tap(slot));
            cell.setOnLongClickListener(v -> {
                pick(slot);
                return true;
            });
            cells[i] = cell;
        }
        refresh();
    }

    private void tap(int slot) {
        Apps.App a = Shortcuts.get(getContext(), slot);
        if (editing || a == null) pick(slot);
        else Apps.get(getContext()).launch(getContext(), a);
    }

    private void pick(final int slot) {
        final Context c = getContext();
        AppPicker.show(c, "Pintasan " + (slot + 1), true, a -> Shortcuts.set(c, slot, a));
    }

    private void setEditing(boolean on) {
        editing = on;
        edit.setImageResource(on ? R.drawable.ic_check : R.drawable.ic_edit);
        edit.setImageTintList(ColorStateList.valueOf(on ? Ui.ACCENT : Ui.TEXT2));
        title.setText(on ? "Pilih pintasan" : "Pintasan");
        for (FrameLayout cell : cells) cell.setBackground(on ? Ui.round(Ui.CHIP, 20) : null);
    }

    private void refresh() {
        for (int i = 0; i < Shortcuts.N; i++) {
            Apps.App a = Shortcuts.get(getContext(), i);
            ImageView iv = icons[i];
            if (a != null && a.icon != null) {
                iv.setImageTintList(null);
                iv.setImageBitmap(a.icon);
                iv.setBackground(null);
                iv.setPadding(0, 0, 0, 0);
                labels[i].setText(a.label);
            } else {
                iv.setImageResource(R.drawable.ic_add);
                iv.setImageTintList(ColorStateList.valueOf(Ui.TEXT3));
                iv.setBackground(Ui.round(Ui.CHIP, 20));
                int pad = Ui.px(24);
                iv.setPadding(pad, pad, pad, pad);
                labels[i].setText(a != null ? a.label : "Tambah");
            }
        }
    }

    @Override public void onAppsChanged() {
        refresh();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        refresh();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Apps.get(getContext()).add(this);
        Shortcuts.prefs(getContext()).registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override protected void onDetachedFromWindow() {
        Apps.get(getContext()).remove(this);
        Shortcuts.prefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDetachedFromWindow();
    }
}
