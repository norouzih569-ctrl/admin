package com.barber.admin;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** تب بازدیدها: جاهایی که رفته‌ایم و دمو داده‌ایم (خودم یا ویزیتور)، چه خریده باشند چه نه. */
public class LeadsTab implements Tab {

    static final String[] STATUS = {"", "⏳ منتظر جواب", "👍 علاقه‌مند", "❌ نخواست", "✅ مشتری شد"};

    /** دلیل‌های «نخواست» (برای آمار) */
    static final String[] REASONS = {"قیمت بالاست", "نیازی ندارد", "فعلاً زود است / بعداً", "نرم‌افزار دیگری دارد",
            "اعتماد نکرد یا ترسید", "دلیل دیگر"};

    /** آیا محله‌ی بازدید جزو محله‌های مجاز ویزیتور است؟ (فهرست خالی = بدون محدودیت) */
    static boolean areaAllowed(String allowed, String area, String address) {
        if (allowed == null || allowed.trim().isEmpty()) return true;
        String hay = norm(area) + " " + norm(address);
        for (String tok : allowed.split("[,،;؛\\n]")) {
            String t = norm(tok);
            if (t.isEmpty()) continue;
            if (hay.contains(t)) return true;
            String a = norm(area);
            if (!a.isEmpty() && t.contains(a)) return true;
        }
        return false;
    }

    private static String norm(String s) {
        return s == null ? "" : s.replace('ي', 'ی').replace('ك', 'ک').trim().toLowerCase();
    }

    private final MainActivity act;
    private final LinearLayout root;
    private final RowAdapter adapter;
    private final EditText etSearch;
    private final Spinner spFilter;
    private final TextView banner;
    private List<Models.Lead> shown = new ArrayList<Models.Lead>();

    public LeadsTab(MainActivity activity) {
        this.act = activity;
        root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        banner = new TextView(act);
        banner.setTextSize(13);
        banner.setTypeface(banner.getTypeface(), android.graphics.Typeface.BOLD);
        banner.setTextColor(Ui.themeColor(act, R.attr.barberAccent, 0xFF1E6BE6));
        banner.setBackground(Ui.tinted(act, Ui.themeColor(act, R.attr.barberAccent, 0xFF1E6BE6), 0x22, 10));
        banner.setPadding(Ui.dp(act, 12), Ui.dp(act, 8), Ui.dp(act, 12), Ui.dp(act, 8));
        banner.setVisibility(View.GONE);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bl.setMargins(Ui.dp(act, 12), Ui.dp(act, 8), Ui.dp(act, 12), 0);
        root.addView(banner, bl);
        banner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spFilter.setSelection(5);
            }
        });

        etSearch = new EditText(act);
        etSearch.setHint("جستجوی نام، مالک، شهر یا شماره");
        etSearch.setSingleLine(true);
        etSearch.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(act, 12), Ui.dp(act, 8), Ui.dp(act, 12), 0);
        root.addView(etSearch, lp);

        spFilter = new Spinner(act);
        spFilter.setAdapter(Ui.spinnerAdapter(act, Arrays.asList(
                "همه‌ی بازدیدها", STATUS[1], STATUS[2], STATUS[3], STATUS[4],
                "📌 پیگیری امروز و عقب‌افتاده")));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(Ui.dp(act, 12), 0, Ui.dp(act, 12), 0);
        root.addView(spFilter, lp2);

        FrameLayout frame = new FrameLayout(act);
        root.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ListView list = new ListView(act);
        list.setClipToPadding(false);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setDrawSelectorOnTop(true);
        list.setSelector(R.drawable.sel_card);
        list.setPadding(0, Ui.dp(act, 4), 0, Ui.dp(act, 88));
        list.setVerticalScrollBarEnabled(false);
        adapter = new RowAdapter(act);
        list.setAdapter(adapter);
        frame.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView empty = new TextView(act);
        empty.setText("هنوز بازدیدی ثبت نشده است.\nبا دکمه‌ی «بازدید جدید» جاهایی را که دمو داده‌اید اضافه کنید.");
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setTextColor(Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888));
        frame.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list.setEmptyView(empty);

        ExtendedFloatingActionButton fab = new ExtendedFloatingActionButton(act);
        fab.setText("بازدید جدید");
        fab.setIconResource(R.drawable.ic_add);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM | android.view.Gravity.END);
        fl.setMargins(Ui.dp(act, 16), Ui.dp(act, 16), Ui.dp(act, 16), Ui.dp(act, 16));
        frame.addView(fab, fl);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForm(null);
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < shown.size()) showActions(shown.get(position));
            }
        });
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                refresh();
            }
        });
    }

    @Override
    public View view() {
        return root;
    }

    @Override
    public void refresh() {
        Db db = Db.get(act);
        String q = Fmt.normalizeDigits(etSearch.getText().toString()).trim().toLowerCase();
        int filter = spFilter.getSelectedItemPosition(); // 0 همه، 1..4 وضعیت، 5 پیگیری
        String today = JalaliCalendar.todayGregorianString();
        List<Models.Lead> all = db.listLeads();
        int due = 0;
        for (Models.Lead x : all) if (isDue(x, today)) due++;
        if (due > 0) {
            banner.setText("📌 " + Fa.digits(String.valueOf(due)) + " پیگیری برای امروز یا عقب‌افتاده دارید (لمس کنید)");
            banner.setVisibility(View.VISIBLE);
        } else {
            banner.setVisibility(View.GONE);
        }
        List<Models.Visitor> visitors = db.listVisitors(false);
        shown = new ArrayList<Models.Lead>();
        List<RowAdapter.Row> rows = new ArrayList<RowAdapter.Row>();
        for (Models.Lead x : all) {
            if (filter == 5) {
                if (!isDue(x, today)) continue;
            } else if (filter > 0 && x.status != filter) continue;
            if (!q.isEmpty()) {
                String hay = (x.shopName + " " + x.ownerName + " " + x.city + " " + x.area + " " + Fmt.normalizeDigits(x.mobile))
                        .toLowerCase();
                if (!hay.contains(q)) continue;
            }
            shown.add(x);
            StringBuilder sub = new StringBuilder();
            if (!x.ownerName.isEmpty()) sub.append(x.ownerName);
            if (!x.area.isEmpty()) sub.append(sub.length() > 0 ? " • " : "").append(x.area);
            if (!x.city.isEmpty()) sub.append(sub.length() > 0 ? " • " : "").append(x.city);
            sub.append(sub.length() > 0 ? " • " : "").append("دمو: ").append(demoBy(x, visitors));
            if (x.status == 3 && !x.declineReason.isEmpty()) sub.append("\nدلیل: ").append(x.declineReason);
            if (!x.followUpDate.isEmpty()) {
                sub.append("\nپیگیری: ").append(JalaliCalendar.gregorianStringToJalali(x.followUpDate));
            }
            rows.add(new RowAdapter.Row(x.id, "", x.shopName, sub.toString(),
                    STATUS[x.status] + "\n" + JalaliCalendar.gregorianStringToJalali(x.visitDate), true));
        }
        adapter.setRows(rows);
    }

    /** پیگیری‌ای که موعدش رسیده و بازدید هنوز به نتیجه‌ی نهایی (نخواست/مشتری شد) نرسیده */
    private static boolean isDue(Models.Lead x, String today) {
        return !x.followUpDate.isEmpty() && x.followUpDate.compareTo(today) <= 0
                && x.status != 3 && x.status != 4;
    }

    private static String demoBy(Models.Lead x, List<Models.Visitor> visitors) {
        if (x.visitorId <= 0) return "خودم";
        for (Models.Visitor v : visitors) if (v.id == x.visitorId) return v.name;
        return "ویزیتور";
    }

    // ---------------- منوی یک بازدید ----------------

    private void showActions(final Models.Lead x) {
        List<String> items = new ArrayList<String>();
        items.add("✏️ ویرایش");
        if (x.customerId <= 0) items.add("🏪 تبدیل به مشتری");
        if (!x.mobile.isEmpty()) items.add("📞 تماس");
        items.add("🗑 حذف");
        final String[] arr = items.toArray(new String[0]);
        new MaterialAlertDialogBuilder(act)
                .setTitle(x.shopName)
                .setItems(arr, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String it = arr[which];
                        if (it.startsWith("✏️")) showForm(x);
                        else if (it.startsWith("🏪")) convert(x);
                        else if (it.startsWith("📞")) {
                            try {
                                act.startActivity(new android.content.Intent(android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:" + Fmt.normalizeDigits(x.mobile))));
                            } catch (Exception e) {
                                Ui.toast(act, "برنامه‌ی تماس پیدا نشد");
                            }
                        } else confirmDelete(x);
                    }
                })
                .show();
    }

    private void confirmDelete(final Models.Lead x) {
        new MaterialAlertDialogBuilder(act)
                .setTitle("حذف بازدید")
                .setMessage("«" + x.shopName + "» از فهرست بازدیدها حذف شود؟")
                .setPositiveButton("حذف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Db.get(act).deleteLead(x.id);
                        refresh();
                        Ui.toast(act, "حذف شد");
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    /** بازدید را به مشتری تبدیل می‌کند (اطلاعاتش کپی می‌شود) و وضعیت «مشتری شد» می‌گیرد. */
    private void convert(final Models.Lead x) {
        new MaterialAlertDialogBuilder(act)
                .setTitle("تبدیل به مشتری")
                .setMessage("«" + x.shopName + "» به فهرست مشتریان اضافه شود؟ بعد از آن می‌توانید برایش لایسنس صادر کنید.")
                .setPositiveButton("اضافه شود", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Db db = Db.get(act);
                        Models.Customer c = new Models.Customer();
                        c.shopName = x.shopName;
                        c.ownerName = x.ownerName;
                        c.mobile = x.mobile;
                        c.city = x.city;
                        c.address = x.address;
                        c.visitorId = x.visitorId;
                        c.metDate = x.visitDate;
                        c.notes = x.notes;
                        x.customerId = db.insertCustomer(c);
                        x.status = 4;
                        db.updateLead(x);
                        act.refreshAll();
                        Ui.toast(act, "به مشتریان اضافه شد");
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    // ---------------- فرم افزودن/ویرایش ----------------

    private EditText field(LinearLayout box, String hint, String value, int type) {
        EditText e = new EditText(act);
        e.setHint(hint);
        e.setInputType(type);
        e.setText(value);
        e.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 8);
        box.addView(e, lp);
        return e;
    }

    private void dateButton(LinearLayout box, final String label, final String[] holder, final boolean clearable) {
        final Button b = new Button(act, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 8);
        box.addView(b, lp);
        final Runnable upd = new Runnable() {
            @Override
            public void run() {
                b.setText(label + ": " + (holder[0].isEmpty() ? "ندارد (لمس کنید)"
                        : JalaliCalendar.gregorianStringToJalali(holder[0])));
            }
        };
        upd.run();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] init = holder[0].isEmpty() ? JalaliCalendar.today() : JalaliCalendar.jalaliOfIso(holder[0]);
                JalaliPicker.show(act, init, new JalaliPicker.OnPicked() {
                    @Override
                    public void onPicked(int y, int m, int d) {
                        holder[0] = JalaliCalendar.jalaliToGregorianString(y, m, d);
                        upd.run();
                    }
                });
            }
        });
        if (clearable) {
            b.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    holder[0] = "";
                    upd.run();
                    return true;
                }
            });
        }
    }

    private void showForm(final Models.Lead existing) {
        final Models.Lead x = existing != null ? existing : new Models.Lead();
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 12), Ui.dp(act, 20), Ui.dp(act, 8));

        final EditText etShop = field(box, "نام آرایشگاه / سالن *", x.shopName,
                android.text.InputType.TYPE_CLASS_TEXT);
        final EditText etOwner = field(box, "نام صاحب", x.ownerName,
                android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText etMobile = field(box, "موبایل", x.mobile, android.text.InputType.TYPE_CLASS_PHONE);
        final EditText etArea = field(box, "محله", x.area, android.text.InputType.TYPE_CLASS_TEXT);
        final EditText etCity = field(box, "شهر", x.city, android.text.InputType.TYPE_CLASS_TEXT);
        final EditText etAddr = field(box, "نشانی", x.address,
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final String[] visit = {x.visitDate.isEmpty() ? JalaliCalendar.todayGregorianString() : x.visitDate};
        dateButton(box, "تاریخ بازدید", visit, false);

        final List<Models.Visitor> visitors = Db.get(act).listVisitors(false);
        List<String> names = new ArrayList<String>();
        names.add("دمو را خودم دادم");
        int sel = 0;
        for (int i = 0; i < visitors.size(); i++) {
            names.add("دمو توسط: " + visitors.get(i).name);
            if (visitors.get(i).id == x.visitorId) sel = i + 1;
        }
        final Spinner spBy = new Spinner(act);
        spBy.setAdapter(Ui.spinnerAdapter(act, names));
        spBy.setSelection(sel);
        box.addView(spBy);

        final Spinner spStatus = new Spinner(act);
        spStatus.setAdapter(Ui.spinnerAdapter(act, Arrays.asList(
                "نتیجه: " + STATUS[1], "نتیجه: " + STATUS[2], "نتیجه: " + STATUS[3], "نتیجه: " + STATUS[4])));
        spStatus.setSelection(Math.max(0, x.status - 1));
        box.addView(spStatus);

        final Spinner spReason = new Spinner(act);
        List<String> rs = new ArrayList<String>();
        for (String r : REASONS) rs.add("دلیل نخواستن: " + r);
        spReason.setAdapter(Ui.spinnerAdapter(act, rs));
        for (int i = 0; i < REASONS.length; i++) if (REASONS[i].equals(x.declineReason)) spReason.setSelection(i);
        spReason.setVisibility(x.status == 3 ? View.VISIBLE : View.GONE);
        box.addView(spReason);
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spReason.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final String[] follow = {x.followUpDate};
        dateButton(box, "تاریخ پیگیری", follow, true);
        TextView hint = new TextView(act);
        hint.setText("برای پاک‌کردن تاریخ پیگیری، روی دکمه‌اش نگه دارید.");
        hint.setTextSize(11);
        hint.setTextColor(Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888));
        box.addView(hint);

        final EditText etNotes = field(box, "یادداشت (مثلاً دلیل نخواستن، قول و قرارها)", x.notes,
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(act)
                .setTitle(existing == null ? "بازدید جدید" : "ویرایش بازدید")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shop = etShop.getText().toString().trim();
                if (shop.isEmpty()) {
                    etShop.setError("نام را وارد کنید");
                    return;
                }
                String mob = etMobile.getText().toString().trim();
                if (!mob.isEmpty() && !Validate.isValidMobile(mob)) {
                    etMobile.setError("موبایل باید مثل 09123456789 باشد");
                    return;
                }
                final int p = spBy.getSelectedItemPosition();
                final Models.Visitor by = p <= 0 ? null : visitors.get(p - 1);
                final String area = etArea.getText().toString().trim();
                final String addr = etAddr.getText().toString();
                if (by != null && !by.areas.trim().isEmpty()) {
                    if (area.isEmpty() && addr.trim().isEmpty()) {
                        etArea.setError("محله را وارد کنید (این ویزیتور فقط برای محله‌های مشخصی مجاز است)");
                        return;
                    }
                    if (!areaAllowed(by.areas, area, addr)) {
                        new MaterialAlertDialogBuilder(act)
                                .setTitle("خارج از محدوده‌ی ویزیتور")
                                .setMessage("«" + by.name + "» فقط برای این محله‌ها مجاز است:\n" + by.areas
                                        + "\n\nمحله‌ی واردشده (" + (area.isEmpty() ? addr : area)
                                        + ") جزو آن‌ها نیست. باز هم ثبت شود؟")
                                .setPositiveButton("بله، ثبت شود", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        save();
                                    }
                                })
                                .setNegativeButton("برگردم", null)
                                .show();
                        return;
                    }
                }
                save();
            }

            private void save() {
                String mob = etMobile.getText().toString().trim();
                x.shopName = etShop.getText().toString().trim();
                x.ownerName = etOwner.getText().toString();
                x.mobile = mob.isEmpty() ? "" : Validate.normalizeMobile(mob);
                x.area = etArea.getText().toString().trim();
                x.city = etCity.getText().toString();
                x.address = etAddr.getText().toString();
                x.visitDate = visit[0];
                int p = spBy.getSelectedItemPosition();
                x.visitorId = p <= 0 ? 0 : visitors.get(p - 1).id;
                x.status = spStatus.getSelectedItemPosition() + 1;
                x.declineReason = x.status == 3 ? REASONS[spReason.getSelectedItemPosition()] : "";
                x.followUpDate = follow[0];
                x.notes = etNotes.getText().toString();
                Db db = Db.get(act);
                if (existing == null) db.insertLead(x);
                else db.updateLead(x);
                dialog.dismiss();
                refresh();
                Ui.toast(act, existing == null ? "بازدید ثبت شد" : "ویرایش شد");
            }
        });
    }
}
