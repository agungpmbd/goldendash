package com.goldendash.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Grid aplikasi 6×2 per halaman; geser kiri/kanan untuk berpindah halaman. */
final class AppGrid extends FrameLayout {
    interface Host {
        void open(Apps.App a);

        void menu(Apps.App a);
    }

    interface PageListener {
        void onPage(int page, int pages);
    }

    static final int COLS = 6, ROWS = 2, PER = COLS * ROWS;
    private static final float CELL_W = 188, CELL_H = 160, TOP = 28, ROW_STEP = 168;

    private final float pageDesign;
    private final Host host;
    private final FrameLayout strip;
    private final Map<String, View> cells = new HashMap<>();
    private final int slop;
    private PageListener pageListener;
    private VelocityTracker vt;
    private int page, pages = 1;
    private float downX, downY, startTx;
    private boolean dragging;

    AppGrid(Context c, float pageWidth, Host host) {
        super(c);
        pageDesign = pageWidth;
        this.host = host;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        strip = new FrameLayout(c);
        addView(strip, new LayoutParams(Ui.px(pageWidth), LayoutParams.MATCH_PARENT));
    }

    void setOnPage(PageListener l) {
        pageListener = l;
    }

    /** Ikon dibuat ulang saat daftar aplikasi dimuat ulang (ikon/label bisa berubah setelah update). */
    void clearCache() {
        cells.clear();
    }

    void setApps(List<Apps.App> list, boolean reset) {
        strip.removeAllViews();
        Map<String, View> keep = new HashMap<>();
        pages = Math.max(1, (list.size() + PER - 1) / PER);
        LayoutParams lp = (LayoutParams) strip.getLayoutParams();
        lp.width = Ui.px(pageDesign) * pages;
        strip.setLayoutParams(lp);
        for (int i = 0; i < list.size(); i++) {
            Apps.App a = list.get(i);
            int p = i / PER, r = (i % PER) / COLS, col = i % COLS;
            View v = cell(a);
            keep.put(a.key(), v);
            strip.addView(v, Ui.at(p * pageDesign + col * CELL_W, TOP + r * ROW_STEP, CELL_W, CELL_H));
        }
        cells.clear();
        cells.putAll(keep);
        if (reset || page >= pages) page = reset ? 0 : pages - 1;
        strip.animate().cancel();
        strip.setTranslationX(-page * pageWidth());
        if (pageListener != null) pageListener.onPage(page, pages);
    }

    private View cell(final Apps.App a) {
        View v = cells.get(a.key());
        if (v != null) return v;
        Context c = getContext();
        FrameLayout f = new FrameLayout(c);
        ImageView iv = Ui.add(f, new ImageView(c), 38, 0, 112, 112);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageBitmap(a.icon);
        TextView t = Ui.add(f, Ui.text(c, 18, Ui.medium, Ui.TEXT), 4, 124, 180, 26);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setText(a.label);
        Ui.pressable(f, 24);
        f.setOnClickListener(x -> host.open(a));
        f.setOnLongClickListener(x -> {
            host.menu(a);
            return true;
        });
        return f;
    }

    private int pageWidth() {
        return Ui.px(pageDesign);
    }

    private void snap(int target) {
        page = Math.max(0, Math.min(pages - 1, target));
        strip.animate().translationX(-page * pageWidth()).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
        if (pageListener != null) pageListener.onPage(page, pages);
    }

    private void track(MotionEvent e) {
        if (vt == null) vt = VelocityTracker.obtain();
        vt.addMovement(e);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        track(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                down(e);
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(e.getX() - downX), dy = Math.abs(e.getY() - downY);
                if (!dragging && pages > 1 && dx > slop && dx > dy) dragging = true;
                return dragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                recycle();
                return false;
            default:
                return dragging;
        }
    }

    private void down(MotionEvent e) {
        downX = e.getX();
        downY = e.getY();
        dragging = false;
        strip.animate().cancel();
        startTx = strip.getTranslationX();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        track(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                down(e);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX;
                if (!dragging && pages > 1 && Math.abs(dx) > slop) dragging = true;
                if (dragging) {
                    float tx = startTx + dx, min = -(pages - 1) * pageWidth();
                    // Tahan di ujung: geseran melewati halaman pertama/terakhir hanya sepertiga.
                    if (tx > 0) tx /= 3f;
                    else if (tx < min) tx = min + (tx - min) / 3f;
                    strip.setTranslationX(tx);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    float dx = e.getX() - downX, vx = 0;
                    if (vt != null) {
                        vt.computeCurrentVelocity(1000);
                        vx = vt.getXVelocity();
                    }
                    int target = page;
                    if (dx < -pageWidth() / 4f || vx < -800) target++;
                    else if (dx > pageWidth() / 4f || vx > 800) target--;
                    snap(target);
                }
                dragging = false;
                recycle();
                return true;
            default:
                return true;
        }
    }

    private void recycle() {
        if (vt != null) {
            vt.recycle();
            vt = null;
        }
    }

    /** Titik penanda halaman di bawah grid. */
    static final class Dots extends View {
        private final Paint on = new Paint(Paint.ANTI_ALIAS_FLAG), off = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int page, pages = 1;

        Dots(Context c) {
            super(c);
            on.setColor(Ui.ACCENT);
            off.setColor(Ui.OFF);
        }

        void set(int p, int n) {
            page = p;
            pages = n;
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            if (pages <= 1) return;
            float r = Ui.pxf(5), gap = Ui.pxf(12), w = pages * 2 * r + (pages - 1) * gap;
            float x = (getWidth() - w) / 2f + r, cy = getHeight() / 2f;
            for (int i = 0; i < pages; i++, x += 2 * r + gap) c.drawCircle(x, cy, r, i == page ? on : off);
        }
    }
}
