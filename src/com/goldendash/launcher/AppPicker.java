package com.goldendash.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Dialog daftar aplikasi untuk memilih isi pintasan. */
final class AppPicker {
    interface Pick {
        void pick(Apps.App app);
    }

    private AppPicker() { }

    static void show(final Context c, String title, boolean allowEmpty, final Pick pick) {
        final List<Apps.App> list = new ArrayList<Apps.App>(Apps.get(c).visible());
        if (allowEmpty) list.add(0, null);
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() {
                return list.size();
            }

            @Override public Object getItem(int i) {
                return list.get(i);
            }

            @Override public long getItemId(int i) {
                return i;
            }

            @Override public View getView(int i, View v, ViewGroup parent) {
                LinearLayout row = v instanceof LinearLayout ? (LinearLayout) v : row(c);
                ImageView iv = (ImageView) row.getChildAt(0);
                TextView tv = (TextView) row.getChildAt(1);
                Apps.App a = list.get(i);
                if (a == null) {
                    iv.setImageResource(R.drawable.ic_close);
                    iv.setImageTintList(ColorStateList.valueOf(Ui.TEXT2));
                    tv.setText("Kosongkan");
                } else {
                    iv.setImageTintList(null);
                    iv.setImageBitmap(a.icon);
                    tv.setText(a.label);
                }
                return row;
            }
        };
        new AlertDialog.Builder(c, Ui.DIALOG)
                .setTitle(title)
                .setAdapter(adapter, (d, which) -> pick.pick(list.get(which)))
                .setNegativeButton("Batal", null)
                .show();
    }

    private static LinearLayout row(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = Ui.px(18);
        row.setPadding(pad, Ui.px(8), pad, Ui.px(8));
        row.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.px(68)));
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(iv, new LinearLayout.LayoutParams(Ui.px(48), Ui.px(48)));
        TextView tv = new TextView(c);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.pxf(20));
        tv.setTextColor(Ui.TEXT);
        tv.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.px(18);
        row.addView(tv, lp);
        return row;
    }
}
