package com.goldendash.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Layar Semua Aplikasi: cari, tab kategori, grid berhalaman, pemutar mini, dan tombol kembali ke Home. */
final class AppsScreen extends FrameLayout implements Apps.Listener, AppGrid.Host {
    private static final String[] TABS = {"Semua", "Navigasi", "Media", "Sistem"};
    private static final int[] TAB_CAT = {-1, Apps.NAV, Apps.MEDIA, Apps.SYSTEM};
    private static final float TAB_W = 164;

    private final EditText search;
    private final TextView[] tabs = new TextView[TABS.length];
    private final View[] seps = new View[TABS.length - 1];
    private final AppGrid grid;
    private final AppGrid.Dots dots;
    private final TextView empty;
    private int tab;

    AppsScreen(Context c, final Sidebar.Nav nav) {
        super(c);
        TextView title = Ui.add(this, Ui.text(c, 52, Ui.bold, Ui.TEXT), 140, 44, 580, 68);
        title.setText("Semua Aplikasi");

        FrameLayout box = Ui.add(this, new FrameLayout(c), 752, 46, 382, 64);
        box.setBackground(Ui.field(32));
        Ui.add(box, Ui.icon(c, R.drawable.ic_search, Ui.TEXT), 22, 16, 32, 32);
        search = Ui.add(box, new EditText(c), 64, 0, 300, 64);
        search.setBackground(null);
        search.setPadding(0, 0, 0, 0);
        search.setSingleLine(true);
        search.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.pxf(22));
        search.setTypeface(Ui.regular);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.TEXT3);
        search.setHint("Cari aplikasi");
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        // Tanpa ini keyboard landscape menutupi seluruh layar dengan kotak teks sendiri.
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int n) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int n) { }

            @Override public void afterTextChanged(Editable s) {
                refilter(true);
            }
        });
        search.setOnEditorActionListener((v, id, ev) -> {
            hideKeyboard();
            return true;
        });

        FrameLayout atur = Ui.add(this, new FrameLayout(c), 1148, 46, 108, 64);
        atur.setBackground(Ui.field(22));
        Ui.add(atur, Ui.icon(c, R.drawable.ic_tune, Ui.TEXT), 16, 18, 28, 28);
        TextView aturText = Ui.add(atur, Ui.text(c, 20, Ui.medium, Ui.TEXT), 52, 0, 54, 64);
        aturText.setGravity(Gravity.CENTER_VERTICAL);
        aturText.setText("Atur");
        Ui.pressable(atur, 22);
        atur.setOnClickListener(v -> arrange());

        FrameLayout bar = Ui.add(this, new FrameLayout(c), 140, 126, TAB_W * TABS.length, 56);
        bar.setBackground(Ui.field(28));
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            TextView t = Ui.add(bar, Ui.text(c, 20, Ui.medium, Ui.TEXT2), i * TAB_W + 4, 4, TAB_W - 8, 48);
            t.setGravity(Gravity.CENTER);
            t.setText(TABS[i]);
            t.setOnClickListener(v -> setTab(k));
            tabs[i] = t;
        }
        for (int i = 0; i < seps.length; i++) {
            seps[i] = Ui.add(bar, new View(c), (i + 1) * TAB_W, 15, 1.5f, 26);
            seps[i].setBackgroundColor(Ui.LINE);
        }

        FrameLayout card = Ui.add(this, new FrameLayout(c), 128, 196, 1128, 396);
        card.setBackground(Ui.card());
        grid = Ui.add(card, new AppGrid(c, 1128, this), 0, 0, 1128, 362);
        dots = Ui.add(card, new AppGrid.Dots(c), 0, 362, 1128, 20);
        empty = Ui.add(card, Ui.text(c, 20, Ui.medium, Ui.TEXT2), 0, 170, 1128, 30);
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setText("Tidak ada aplikasi");
        grid.setOnPage(dots::set);

        Ui.add(this, new MiniPlayer(c), 128, 606, 980, 90);
        FrameLayout homeBtn = Ui.add(this, new FrameLayout(c), 1122, 606, 134, 90);
        homeBtn.setBackground(Ui.card());
        Ui.add(homeBtn, Ui.icon(c, R.drawable.ic_home, Ui.TEXT2), 47, 12, 40, 40);
        TextView homeText = Ui.add(homeBtn, Ui.text(c, 18, Ui.medium, Ui.TEXT), 0, 58, 134, 24);
        homeText.setGravity(Gravity.CENTER_HORIZONTAL);
        homeText.setText("Home");
        Ui.pressable(homeBtn, 26);
        homeBtn.setOnClickListener(v -> nav.go(Sidebar.HOME));

        setTab(0);
    }

    private void setTab(int i) {
        tab = i;
        for (int k = 0; k < tabs.length; k++) {
            boolean on = k == i;
            tabs[k].setBackground(on ? Ui.accent(24) : null);
            tabs[k].setTextColor(on ? Ui.WHITE : Ui.TEXT2);
        }
        for (int k = 0; k < seps.length; k++) seps[k].setVisibility(k == i || k + 1 == i ? View.INVISIBLE : View.VISIBLE);
        refilter(true);
    }

    private void refilter(boolean resetPage) {
        Apps apps = Apps.get(getContext());
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int cat = TAB_CAT[tab];
        final List<Apps.App> out = new ArrayList<>();
        for (Apps.App a : apps.visible()) {
            if (cat >= 0 && a.cat != cat) continue;
            if (!q.isEmpty() && !a.label.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(a);
        }
        if (apps.prefs().getBoolean("byUse", false)) {
            // Urutan stabil: aplikasi dengan jumlah dibuka sama tetap urut abjad.
            Collections.sort(out, (x, y) -> Integer.compare(apps.count(y), apps.count(x)));
        }
        grid.setApps(out, resetPage);
        empty.setVisibility(out.isEmpty() && apps.loaded() ? View.VISIBLE : View.GONE);
    }

    void hideKeyboard() {
        if (!search.hasFocus()) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
    }

    @Override public void open(Apps.App a) {
        hideKeyboard();
        Apps.get(getContext()).launch(getContext(), a);
    }

    @Override public void menu(final Apps.App a) {
        final Context c = getContext();
        String[] items = {"Tambahkan ke Pintasan", "Info aplikasi", "Sembunyikan"};
        new AlertDialog.Builder(c, Ui.DIALOG).setTitle(a.label).setItems(items, (d, w) -> {
            if (w == 0) {
                addToShortcut(a);
            } else if (w == 1) {
                Ui.start(c, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", a.pkg(), null)));
            } else {
                Apps.get(c).setHidden(a, true);
                refilter(false);
                Toast.makeText(c, a.label + " disembunyikan (kembalikan lewat Atur)", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void addToShortcut(final Apps.App a) {
        final Context c = getContext();
        String[] slots = new String[Shortcuts.N];
        for (int i = 0; i < slots.length; i++) {
            Apps.App cur = Shortcuts.get(c, i);
            slots[i] = (i + 1) + ". " + (cur != null ? cur.label : "Kosong");
        }
        new AlertDialog.Builder(c, Ui.DIALOG).setTitle("Ganti pintasan mana?").setItems(slots, (d, w) -> {
            Shortcuts.set(c, w, a);
            Toast.makeText(c, a.label + " ditambahkan ke pintasan", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Batal", null).show();
    }

    private void arrange() {
        final Context c = getContext();
        final SharedPreferences p = Apps.get(c).prefs();
        boolean byUse = p.getBoolean("byUse", false);
        String[] items = {
            (byUse ? "" : "✓ ") + "Urutkan A–Z",
            (byUse ? "✓ " : "") + "Urutkan paling sering dibuka",
            "Aplikasi tersembunyi (" + Apps.get(c).hiddenApps().size() + ")",
        };
        new AlertDialog.Builder(c, Ui.DIALOG).setTitle("Atur").setItems(items, (d, w) -> {
            if (w < 2) {
                p.edit().putBoolean("byUse", w == 1).apply();
                refilter(true);
            } else {
                hiddenDialog();
            }
        }).show();
    }

    private void hiddenDialog() {
        final Context c = getContext();
        final List<Apps.App> hidden = Apps.get(c).hiddenApps();
        if (hidden.isEmpty()) {
            Toast.makeText(c, "Tidak ada aplikasi tersembunyi", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[hidden.size()];
        final boolean[] keep = new boolean[hidden.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = hidden.get(i).label;
            keep[i] = true;
        }
        new AlertDialog.Builder(c, Ui.DIALOG).setTitle("Hapus centang untuk menampilkan lagi")
                .setMultiChoiceItems(names, keep, (d, w, on) -> keep[w] = on)
                .setPositiveButton("Simpan", (d, w) -> {
                    for (int i = 0; i < keep.length; i++) if (!keep[i]) Apps.get(c).setHidden(hidden.get(i), false);
                    refilter(false);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override public void onAppsChanged() {
        grid.clearCache();
        refilter(false);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Apps.get(getContext()).add(this);
        refilter(false);
    }

    @Override protected void onDetachedFromWindow() {
        Apps.get(getContext()).remove(this);
        super.onDetachedFromWindow();
    }
}
