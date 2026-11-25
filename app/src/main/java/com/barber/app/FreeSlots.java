package com.barber.app;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * نمای «وقت‌های خالی» برای وقتی که مشتری تلفنی وقت می‌خواهد.
 * ساعت کاری روز به بخش‌های «نوبت» و «خالی» تقسیم می‌شود؛ خالی‌ها قرمز نشان داده می‌شوند و
 * با لمس یک وقت خالی، پنجره‌ی نوبت جدید با همان ساعت باز می‌شود. اولین وقت خالی بالا نوشته شده است.
 */
public final class FreeSlots {

    private FreeSlots() {
    }

    /**
     * بخش‌های خالیِ برخوردکننده با زمان بسته را «بسته» (نوع ۲) می‌کند. در حالت آزاد بخش خالی تکه می‌شود؛
     * در حالت ساعتی/نیم‌ساعتی هر خانه‌ای که با زمان بسته برخورد کند کامل بسته می‌شود.
     */
    private static List<int[]> applyClosed(List<int[]> segs, List<Db.ClosedTime> blocks, int mode,
                                           List<Db.ClosedTime> refs) {
        List<int[]> out = new ArrayList<>();
        for (int[] s : segs) {
            if (s[2] != 0) {
                out.add(s);
                continue;
            }
            List<Db.ClosedTime> hit = new ArrayList<>();
            for (Db.ClosedTime ct : blocks) {
                if (s[0] < ct.endMin && s[1] > ct.startMin) hit.add(ct);
            }
            if (hit.isEmpty()) {
                out.add(s);
                continue;
            }
            if (mode != Logic.MODE_FREE) {
                refs.add(hit.get(0));
                out.add(new int[]{s[0], s[1], 2, refs.size() - 1});
                continue;
            }
            java.util.Collections.sort(hit, new java.util.Comparator<Db.ClosedTime>() {
                @Override
                public int compare(Db.ClosedTime a, Db.ClosedTime b) {
                    return a.startMin - b.startMin;
                }
            });
            int cur = s[0];
            for (Db.ClosedTime ct : hit) {
                int cs = Math.max(ct.startMin, cur);
                int ce = Math.min(ct.endMin, s[1]);
                if (ce <= cs) continue;
                if (cs - cur >= 10) out.add(new int[]{cur, cs, 0, -1});
                refs.add(ct);
                out.add(new int[]{cs, ce, 2, refs.size() - 1});
                cur = ce;
            }
            if (s[1] - cur >= 10) out.add(new int[]{cur, s[1], 0, -1});
        }
        return out;
    }

    public static void show(final MainActivity act, final AppointmentsTab tab) {
        final int[] date = act.selectedDate.clone();
        final int mode = AppConfig.getTimeMode(act);
        final int workStart = AppConfig.getWorkStartHour(act) * 60;
        final int workEnd = AppConfig.getWorkEndHour(act) * 60;
        final List<int[]> segs = new ArrayList<>();
        final List<Db.Appointment> appts = new ArrayList<>();
        final List<Db.ClosedTime> closedRefs = new ArrayList<>();
        final boolean[] dayClosed = {false};

        int pad = Ui.dp(act, 12);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        Ui.rtl(box);

        LinearLayout nav = new LinearLayout(act);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        final Button prev = new Button(act);
        prev.setText("روز قبل");
        final Button next = new Button(act);
        next.setText("روز بعد");
        final TextView tvDay = new TextView(act);
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setTextSize(16);
        tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
        nav.addView(prev, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        nav.addView(tvDay, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        nav.addView(next, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(nav);

        final TextView tvFirst = new TextView(act);
        tvFirst.setPadding(0, Ui.dp(act, 6), 0, Ui.dp(act, 6));
        tvFirst.setTextSize(15);
        tvFirst.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(tvFirst);

        final ListView lv = new ListView(act);
        lv.setDivider(null);
        box.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(act, 360)));

        final BaseAdapter ad = new BaseAdapter() {
            @Override
            public int getCount() {
                return segs.size();
            }

            @Override
            public Object getItem(int i) {
                return segs.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View convert, ViewGroup parent) {
                TextView tv = convert instanceof TextView ? (TextView) convert : new TextView(act);
                int[] s = segs.get(i);
                String range = Logic.fromMinutes(s[0]) + " – " + Logic.fromMinutes(s[1]);
                int p = Ui.dp(act, 10);
                tv.setPadding(p, p, p, p);
                tv.setTextSize(15);
                tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                tv.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                if (s[2] == 0) {
                    tv.setText("🔴  " + range + "   خالی   (" + (s[1] - s[0]) + " دقیقه)");
                    tv.setTextColor(0xFFC62828);
                    tv.setBackground(Ui.tinted(act, 0xFFE53935, 0x26, 10));
                } else if (s[2] == 2) {
                    Db.ClosedTime ct = closedRefs.get(s[3]);
                    tv.setText("🚫  " + range + "   " + (ct.allDay ? "تعطیل" : "بسته")
                            + (ct.note.isEmpty() ? "" : "  •  " + ct.note));
                    tv.setTextColor(Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF777777));
                    tv.setBackground(Ui.tinted(act, 0xFF9E9E9E, 0x33, 10));
                } else {
                    Db.Appointment a = appts.get(s[3]);
                    tv.setText("⏱  " + range + "   " + a.customerName + "  •  " + a.service);
                    tv.setTextColor(Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222));
                    tv.setBackground(Ui.tinted(act, 0xFF888888, 0x1A, 10));
                }
                return tv;
            }
        };
        lv.setAdapter(ad);

        final Runnable reload = new Runnable() {
            @Override
            public void run() {
                tvDay.setText(JalaliCalendar.DAY_NAMES[JalaliCalendar.dayOfWeek(date[0], date[1], date[2])]
                        + " " + JalaliCalendar.format(date[0], date[1], date[2]));
                String iso = JalaliCalendar.jalaliToGregorianString(date[0], date[1], date[2]);
                appts.clear();
                appts.addAll(Db.get(act).getAppointments(iso, null));
                int[] st = new int[appts.size()];
                int[] du = new int[appts.size()];
                for (int i = 0; i < st.length; i++) {
                    st[i] = Logic.toMinutes(appts.get(i).time);
                    du[i] = appts.get(i).duration;
                }
                segs.clear();
                segs.addAll(Logic.daySegments(workStart, workEnd, mode, st, du));
                closedRefs.clear();
                dayClosed[0] = false;
                if (AppConfig.bookingEdition()) {
                    List<Db.ClosedTime> blocks = new ArrayList<>();
                    for (Db.ClosedTime ct : Db.get(act).getClosedTimes()) {
                        if (!ct.appliesTo(iso)) continue;
                        if (ct.allDay) {
                            dayClosed[0] = true;
                            Db.ClosedTime full = new Db.ClosedTime();
                            full.allDay = true;
                            full.note = ct.note;
                            full.startMin = 0;
                            full.endMin = 1440;
                            blocks.add(full);
                        } else {
                            blocks.add(ct);
                        }
                    }
                    if (!blocks.isEmpty()) {
                        List<int[]> merged = applyClosed(segs, blocks, mode, closedRefs);
                        segs.clear();
                        segs.addAll(merged);
                    }
                }
                int first = -1;
                for (int i = 0; i < segs.size(); i++) {
                    if (segs.get(i)[2] == 0) {
                        first = i;
                        break;
                    }
                }
                if (first < 0 && dayClosed[0]) {
                    tvFirst.setText("🚫 این روز تعطیل است");
                    tvFirst.setTextColor(0xFF777777);
                } else if (first < 0) {
                    tvFirst.setText("این روز وقت خالی ندارد");
                    tvFirst.setTextColor(0xFFC62828);
                } else {
                    tvFirst.setText("✅ اولین وقت خالی: " + Logic.fromMinutes(segs.get(first)[0]));
                    tvFirst.setTextColor(Ui.themeColor(act, R.attr.barberAccent, 0xFF1E6BE6));
                }
                ad.notifyDataSetChanged();
                if (first >= 0) lv.setSelection(Math.max(0, first - 1));
            }
        };
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] d = JalaliCalendar.addDays(date[0], date[1], date[2], -1);
                System.arraycopy(d, 0, date, 0, 3);
                reload.run();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] d = JalaliCalendar.addDays(date[0], date[1], date[2], 1);
                System.arraycopy(d, 0, date, 0, 3);
                reload.run();
            }
        });
        reload.run();

        final AlertDialog[] holder = new AlertDialog[1];
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                int[] s = segs.get(pos);
                if (s[2] == 2) {
                    Ui.toast(act, "این زمان بسته است؛ در صفحه‌ی «زمان‌های بسته» می‌توانید تغییرش دهید");
                    return;
                }
                if (s[2] != 0) {
                    Ui.toast(act, "این ساعت نوبت دارد");
                    return;
                }
                holder[0].dismiss();
                act.selectedDate = date.clone();
                tab.refresh();
                // بعد از ثبت یا انصراف، دوباره به همین لیست برمی‌گردیم تا رنگ‌ها به‌روز دیده شوند
                tab.openNew(s[0], new Runnable() {
                    @Override
                    public void run() {
                        show(act, tab);
                    }
                });
            }
        });

        holder[0] = new MaterialAlertDialogBuilder(act)
                .setTitle("🕒 وقت‌های خالی")
                .setView(box)
                .setNegativeButton("بستن", null)
                .create();
        holder[0].show();
    }
}
