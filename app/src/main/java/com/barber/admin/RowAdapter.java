package com.barber.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** آداپتور عمومی همه لیست‌ها: (شروع) عنوان کوچک، (وسط) عنوان و توضیح، (انتها) مقدار. */
public class RowAdapter extends BaseAdapter {

    public static class Row {
        public final long id;
        public final String lead;
        public final String title;
        public final String subtitle;
        public final String trail;
        /** اگر true باشد به‌جای متن lead، حرف اول عنوان داخل یک دایره (آواتار) نمایش داده می‌شود */
        public final boolean avatar;
        /** سرتیتر روز در نمای هفتگی (کارت رنگی بدون لبه) */
        public final boolean header;

        public Row(long id, String lead, String title, String subtitle, String trail) {
            this(id, lead, title, subtitle, trail, false);
        }

        public Row(long id, String lead, String title, String subtitle, String trail, boolean avatar) {
            this(id, lead, title, subtitle, trail, avatar, false);
        }

        public Row(long id, String lead, String title, String subtitle, String trail,
                   boolean avatar, boolean header) {
            this.header = header;
            this.id = id;
            this.lead = lead;
            this.title = title;
            this.subtitle = subtitle;
            this.trail = trail;
            this.avatar = avatar;
        }
    }

    private final Context ctx;
    private final List<Row> rows = new ArrayList<>();

    public RowAdapter(Context ctx) {
        this.ctx = ctx;
    }

    private android.widget.GridView gv;
    private boolean grid = false;

    /** لیست/گرید: GridView با یک ستون = نمای کارتی، با دو ستون = نمای گرید */
    public void attach(android.widget.GridView g) {
        gv = g;
        syncMode();
    }

    private void syncMode() {
        grid = AppConfig.isGrid(ctx);
        if (gv != null) gv.setNumColumns(grid ? 2 : 1);
    }

    public void setRows(List<Row> newRows) {
        syncMode();
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rows.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(ctx).inflate(R.layout.item_row, parent, false);
        }
        bind(v, rows.get(position));
        applyMode(v, grid);
        return v;
    }

    /** چیدمان کارت: افقی (کارتی) یا عمودیِ وسط‌چین (گرید) */
    private static void applyMode(View v, boolean grid) {
        Context c = v.getContext();
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) v.findViewById(R.id.row_card);
        android.widget.LinearLayout line = (android.widget.LinearLayout) card.getChildAt(0);
        View lead = line.getChildAt(0);
        View mid = line.getChildAt(1);
        View trail = line.getChildAt(2);
        line.setOrientation(grid ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL);
        line.setGravity(grid ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams lp0 = (android.widget.LinearLayout.LayoutParams) lead.getLayoutParams();
        android.widget.LinearLayout.LayoutParams lp1 = (android.widget.LinearLayout.LayoutParams) mid.getLayoutParams();
        android.widget.LinearLayout.LayoutParams lp2 = (android.widget.LinearLayout.LayoutParams) trail.getLayoutParams();
        lp0.setMarginEnd(grid ? 0 : Ui.dp(c, 12));
        lp0.bottomMargin = grid ? Ui.dp(c, 6) : 0;
        lp1.width = grid ? android.view.ViewGroup.LayoutParams.MATCH_PARENT : 0;
        lp1.weight = grid ? 0 : 1;
        lp2.setMarginStart(grid ? 0 : Ui.dp(c, 12));
        lp2.topMargin = grid ? Ui.dp(c, 6) : 0;
        lead.setLayoutParams(lp0);
        mid.setLayoutParams(lp1);
        trail.setLayoutParams(lp2);
        int al = grid ? View.TEXT_ALIGNMENT_CENTER : View.TEXT_ALIGNMENT_VIEW_START;
        ((TextView) v.findViewById(R.id.row_title)).setTextAlignment(al);
        ((TextView) v.findViewById(R.id.row_subtitle)).setTextAlignment(al);
        v.setPadding(Ui.dp(c, grid ? 6 : 12), v.getPaddingTop(), Ui.dp(c, grid ? 6 : 12), v.getPaddingBottom());
    }

    /** پر کردن یک ردیف (برای استفاده در لیست‌های ساخته‌شده با کد هم کاربرد دارد). */
    public static void bind(View v, Row r) {
        bindLead(v, r);
        bindCard(v, r);
        setText((TextView) v.findViewById(R.id.row_title), r.title);
        setText((TextView) v.findViewById(R.id.row_subtitle), r.subtitle);
        setText((TextView) v.findViewById(R.id.row_trail), r.trail);
    }

    /** سرتیتر روز رنگی و بی‌لبه است؛ ردیف‌های عادی سطح و لبه‌ی تم را دارند (ردیف‌ها بازیافت می‌شوند). */
    private static void bindCard(View v, Row r) {
        Context ctx = v.getContext();
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) v.findViewById(R.id.row_card);
        TextView title = (TextView) v.findViewById(R.id.row_title);
        int accent = Ui.themeColor(ctx, R.attr.barberAccent, 0xFF1E6BE6);
        if (r.header) {
            card.setCardBackgroundColor((accent & 0x00FFFFFF) | (0x26 << 24));
            card.setStrokeWidth(0);
            title.setTextColor(accent);
        } else {
            card.setCardBackgroundColor(Ui.themeColor(ctx, R.attr.barberSurface, 0xFFFFFFFF));
            card.setStrokeWidth(Ui.dp(ctx, 1));
            title.setTextColor(Ui.themeColor(ctx, android.R.attr.textColorPrimary, 0xFF000000));
        }
    }

    /** سمت شروع کارت: آواتار گرد (حرف اول) یا چیپ رنگی (ساعت/تاریخ) */
    private static void bindLead(View v, Row r) {
        Context ctx = v.getContext();
        TextView lead = (TextView) v.findViewById(R.id.row_lead);
        int accent = Ui.themeColor(ctx, R.attr.barberAccent, 0xFF1E6BE6);
        android.widget.LinearLayout.LayoutParams lp =
                (android.widget.LinearLayout.LayoutParams) lead.getLayoutParams();
        if (r.avatar) {
            String t = r.title == null ? "" : r.title.trim();
            String initial = t.isEmpty() ? "؟" : new String(Character.toChars(t.codePointAt(0)));
            lead.setVisibility(View.VISIBLE);
            lead.setText(initial);
            lead.setTextSize(18);
            lp.width = Ui.dp(ctx, 44);
            lp.height = Ui.dp(ctx, 44);
            lead.setPadding(0, 0, 0, 0);
            android.graphics.drawable.GradientDrawable g = Ui.tinted(ctx, accent, 0x2E, 22);
            lead.setBackground(g);
        } else if (r.lead == null || r.lead.isEmpty()) {
            lead.setVisibility(View.GONE);
        } else {
            lead.setVisibility(View.VISIBLE);
            lead.setText(r.lead);
            lead.setTextSize(13);
            lp.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            lead.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
            lead.setBackground(Ui.tinted(ctx, accent, 0x26, 10));
        }
        lead.setLayoutParams(lp);
    }

    private static void setText(TextView tv, String text) {
        if (text == null || text.isEmpty()) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.VISIBLE);
            tv.setText(text);
        }
    }
}
