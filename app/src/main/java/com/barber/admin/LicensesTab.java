package com.barber.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** تب لایسنس‌ها: تاریخچه‌ی همه‌ی کدهای صادرشده و صدور کد جدید. */
public class LicensesTab implements Tab {

    private final MainActivity act;
    private final View root;
    private final RowAdapter adapter;
    private List<Models.License> data = new ArrayList<Models.License>();
    private final Spinner spMonth;
    private List<String> monthKeys = new ArrayList<String>(); // "" = همه
    private String selMonth = "";
    private boolean building = false;

    public LicensesTab(MainActivity activity) {
        this.act = activity;
        root = LayoutInflater.from(act).inflate(R.layout.tab_licenses, null, false);
        android.widget.GridView list = root.findViewById(R.id.list_licenses);
        TextView empty = root.findViewById(R.id.empty_licenses);
        spMonth = root.findViewById(R.id.sp_month);
        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (building || position < 0 || position >= monthKeys.size()) return;
                String k = monthKeys.get(position);
                if (!k.equals(selMonth)) {
                    selMonth = k;
                    refresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        adapter = new RowAdapter(act);
        list.setAdapter(adapter);
        adapter.attach(list);
        list.setEmptyView(empty);
        root.findViewById(R.id.fab_issue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IssueDialog.chooser(act, 0);
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < data.size()) showDetail(data.get(position));
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
        List<Models.License> all = db.listLicenses(0);
        // ماه‌های موجود (جدیدترین اول؛ لیست از قبل بر اساس تاریخ نزولی است)
        List<String> keys = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        keys.add("");
        names.add("همه‌ی ماه‌ها");
        for (Models.License l : all) {
            String k;
            String nm;
            try {
                int[] j = JalaliCalendar.jalaliOfIso(l.issueDate);
                k = j[0] + "/" + j[1];
                nm = JalaliCalendar.MONTH_NAMES[j[1] - 1] + " " + j[0];
            } catch (Exception e) {
                continue;
            }
            if (!keys.contains(k)) {
                keys.add(k);
                names.add(nm);
            }
        }
        if (!keys.contains(selMonth)) selMonth = "";
        if (!keys.equals(monthKeys)) {
            building = true;
            monthKeys = keys;
            spMonth.setAdapter(Ui.spinnerAdapter(act, names));
            spMonth.setSelection(keys.indexOf(selMonth));
            building = false;
        }
        data = new ArrayList<Models.License>();
        for (Models.License l : all) {
            if (selMonth.isEmpty()) {
                data.add(l);
                continue;
            }
            try {
                int[] j = JalaliCalendar.jalaliOfIso(l.issueDate);
                if (selMonth.equals(j[0] + "/" + j[1])) data.add(l);
            } catch (Exception e) {
                // نادیده
            }
        }
        List<RowAdapter.Row> rows = new ArrayList<RowAdapter.Row>();
        for (Models.License l : data) {
            Models.Customer c = db.getCustomer(l.customerId);
            String sub = Labels.licenseType(l.type) + (Labels.isFree(l.type) ? ""
                    : " • " + Fmt.price(l.finalPrice));
            String trail = l.voided ? "⛔ باطل" : (l.code.isEmpty() ? "" : l.code);
            rows.add(new RowAdapter.Row(l.id, JalaliCalendar.gregorianStringToJalali(l.issueDate),
                    c != null ? c.shopName : "؟", sub, trail));
        }
        adapter.setRows(rows);
    }

    private void showDetail(final Models.License l) {
        final Db db = Db.get(act);
        final Models.Customer c = db.getCustomer(l.customerId);
        StringBuilder sb = new StringBuilder();
        sb.append("مشتری: ").append(c != null ? c.shopName : "؟").append('\n');
        sb.append("نوع: ").append(Labels.licenseType(l.type)).append('\n');
        sb.append("تاریخ صدور: ").append(JalaliCalendar.gregorianStringToJalali(l.issueDate)).append('\n');
        sb.append("شناسه‌ی دستگاه: ").append(l.deviceId).append('\n');
        if (!Labels.isFree(l.type)) {
            sb.append("قیمت پایه: ").append(Fmt.money(l.basePrice)).append('\n');
            if (l.discountPercent > 0) sb.append("تخفیف: ").append(Fmt.percent(l.discountPercent)).append('\n');
            sb.append("مبلغ نهایی: ").append(Fmt.money(l.finalPrice)).append('\n');
            if (l.visitorId > 0) {
                Models.Visitor v = db.getVisitor(l.visitorId);
                sb.append("ویزیتور: ").append(v != null ? v.name : "؟").append(" (")
                        .append(Fmt.percent(l.commissionPercent)).append(" = ")
                        .append(Fmt.money(l.commissionAmount)).append(")\n");
            }
        }
        if (!l.notes.isEmpty()) sb.append("یادداشت: ").append(l.notes).append('\n');
        if (l.voided) sb.append("\n⛔ این لایسنس باطل شده و در درآمد و پورسانت حساب نمی‌شود.\n");
        sb.append("\nکد: ").append(l.code);

        new MaterialAlertDialogBuilder(act)
                .setTitle("لایسنس " + JalaliCalendar.gregorianStringToJalali(l.issueDate))
                .setMessage(sb.toString())
                .setPositiveButton("ارسال کد", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        IssueDialog.sendChooser(act, c, IssueLogic.message(l.code, l.type, l.deviceId));
                    }
                })
                .setNeutralButton(l.voided ? "رفع ابطال" : "ابطال", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        confirmVoid(l);
                    }
                })
                .setNegativeButton("ویرایش", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        showEdit(l);
                    }
                })
                .show();
    }

    private static double num(android.widget.EditText et) {
        Double v = Fmt.parsePrice(et.getText().toString());
        return v == null ? 0 : v;
    }

    private static android.widget.EditText addField(android.widget.LinearLayout box, Context c,
                                                    String label, String value, boolean number) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setPadding(0, Ui.dp(c, 8), 0, 0);
        box.addView(t);
        android.widget.EditText et = new android.widget.EditText(c);
        if (number) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(value);
        box.addView(et);
        return et;
    }

    private void showEdit(final Models.License l) {
        final Db db = Db.get(act);
        final boolean free = Labels.isFree(l.type);
        final List<Models.Customer> customers = db.listCustomers("");
        List<String> names = new ArrayList<String>();
        int sel = 0;
        for (int i = 0; i < customers.size(); i++) {
            names.add(customers.get(i).shopName);
            if (customers.get(i).id == l.customerId) sel = i;
        }
        final List<Models.PriceRow> plans = new ArrayList<Models.PriceRow>();
        List<String> planNames = new ArrayList<String>();
        int planSel = 0;
        if (!free) {
            for (Models.PriceRow p : db.getPrices()) {
                if (LicenseCodec.daysForType(p.type) > 0 && !Labels.isFree(p.type)) {
                    if (p.type == l.type) planSel = plans.size();
                    plans.add(p);
                    planNames.add(Labels.licenseType(p.type));
                }
            }
        }
        final List<Models.Visitor> visitors = new ArrayList<Models.Visitor>(db.listVisitors(true));
        List<String> vNames = new ArrayList<String>();
        vNames.add("بدون ویزیتور");
        int vSel = 0;
        for (int i = 0; i < visitors.size(); i++) {
            vNames.add(visitors.get(i).name);
            if (visitors.get(i).id == l.visitorId) vSel = i + 1;
        }
        final String[] date = {l.issueDate};

        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 8), Ui.dp(act, 20), 0);
        TextView t1 = new TextView(act);
        t1.setText("مشتری");
        box.addView(t1);
        final Spinner sp = new Spinner(act);
        sp.setAdapter(Ui.spinnerAdapter(act, names));
        sp.setSelection(sel);
        box.addView(sp);

        final Button bDate = new Button(act);
        bDate.setText("تاریخ صدور: " + JalaliCalendar.gregorianStringToJalali(date[0]));
        bDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JalaliPicker.show(act, JalaliCalendar.jalaliOfIso(date[0]), new JalaliPicker.OnPicked() {
                    @Override
                    public void onPicked(int y, int m, int d) {
                        date[0] = JalaliCalendar.jalaliToGregorianString(y, m, d);
                        bDate.setText("تاریخ صدور: " + JalaliCalendar.format(y, m, d));
                    }
                });
            }
        });
        box.addView(bDate);

        final Spinner spPlan = new Spinner(act);
        final android.widget.EditText etDev;
        final android.widget.EditText etBase;
        final android.widget.EditText etDisc;
        final Spinner spVis = new Spinner(act);
        final android.widget.EditText etComm;
        if (free) {
            etDev = null;
            etBase = null;
            etDisc = null;
            etComm = null;
            TextView info = new TextView(act);
            info.setText("لایسنس آزمایشی/جابجایی: فقط مشتری، تاریخ و توضیحات قابل ویرایش است.");
            info.setPadding(0, Ui.dp(act, 8), 0, 0);
            box.addView(info);
        } else {
            TextView tp = new TextView(act);
            tp.setText("نوع لایسنس (تغییر نوع، کد جدید می‌سازد)");
            tp.setPadding(0, Ui.dp(act, 8), 0, 0);
            box.addView(tp);
            spPlan.setAdapter(Ui.spinnerAdapter(act, planNames));
            spPlan.setSelection(planSel);
            box.addView(spPlan);
            etDev = addField(box, act, "شناسه‌ی دستگاه (تغییر آن کد جدید می‌سازد)", l.deviceId, false);
            etBase = addField(box, act, "قیمت پایه (تومان)", "", true);
            MoneyWatcher.attach(etBase);
            etBase.setText(String.valueOf(l.basePrice));
            etDisc = addField(box, act, "تخفیف (٪)", l.discountPercent == 0 ? "" : Fmt.percent(l.discountPercent).replace("٪", "").replace("%", "").trim(), true);
            TextView tv = new TextView(act);
            tv.setText("ویزیتور");
            tv.setPadding(0, Ui.dp(act, 8), 0, 0);
            box.addView(tv);
            spVis.setAdapter(Ui.spinnerAdapter(act, vNames));
            spVis.setSelection(vSel);
            box.addView(spVis);
            etComm = addField(box, act, "درصد پورسانت ویزیتور (٪)", l.commissionPercent == 0 ? "" : Fmt.percent(l.commissionPercent).replace("٪", "").replace("%", "").trim(), true);
        }
        final android.widget.EditText et = addField(box, act, "توضیحات", l.notes, false);
        et.setMinLines(2);
        et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);

        final androidx.appcompat.app.AlertDialog dlg = new MaterialAlertDialogBuilder(act)
                .setTitle("ویرایش لایسنس")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .create();
        dlg.show();
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = sp.getSelectedItemPosition();
                if (i < 0 || i >= customers.size()) return;
                final Models.License x = new Models.License();
                x.id = l.id;
                x.customerId = customers.get(i).id;
                x.deviceId = l.deviceId;
                x.type = l.type;
                x.code = l.code;
                x.issueDate = date[0];
                x.basePrice = l.basePrice;
                x.discountPercent = l.discountPercent;
                x.visitorId = l.visitorId;
                x.commissionPercent = l.commissionPercent;
                x.notes = et.getText().toString();
                boolean newCode = false;
                if (!free) {
                    String dev = IssueLogic.parseDevice(etDev.getText().toString());
                    if (dev == null) {
                        etDev.setError("شناسه باید ۱۰ حرف/رقم باشد (مثل K7M2A-9QX4T)");
                        return;
                    }
                    int pi = spPlan.getSelectedItemPosition();
                    if (pi < 0 || pi >= plans.size()) return;
                    Models.PriceRow plan = plans.get(pi);
                    x.deviceId = dev;
                    x.type = plan.type;
                    long base = Math.round(num(etBase));
                    // اگر نوع عوض شده ولی قیمت دست نخورده، قیمت پایه‌ی نوع جدید گذاشته شود
                    if (plan.type != l.type && base == l.basePrice) base = plan.price;
                    x.basePrice = base;
                    x.discountPercent = Pricing.clampPercent(num(etDisc));
                    int vp = spVis.getSelectedItemPosition();
                    x.visitorId = vp > 0 ? visitors.get(vp - 1).id : 0;
                    x.commissionPercent = Pricing.clampPercent(num(etComm));
                    if (!dev.equals(l.deviceId) || plan.type != l.type || !date[0].equals(l.issueDate)) {
                        String code = IssueLogic.makeCode(AppConfig.SECRET, dev, plan.type,
                                IssueLogic.epochDayOfIso(date[0]));
                        if (code == null) {
                            Ui.toastLong(act, "با این تاریخ/شناسه کد ساخته نشد");
                            return;
                        }
                        x.code = code;
                        newCode = !code.equals(l.code);
                    }
                }
                Db.get(act).updateLicenseEdit(x);
                dlg.dismiss();
                act.refreshAll();
                if (newCode) {
                    new MaterialAlertDialogBuilder(act)
                            .setTitle("کد جدید ساخته شد")
                            .setMessage(x.code + "\n\nکد قبلی دیگر مربوط به این مشخصات نیست؛ کد جدید را برای مشتری بفرستید.")
                            .setPositiveButton("ارسال کد", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface d2, int w) {
                                    IssueDialog.sendChooser(act, Db.get(act).getCustomer(x.customerId),
                                            IssueLogic.message(x.code, x.type, x.deviceId));
                                }
                            })
                            .setNegativeButton("بستن", null)
                            .show();
                } else {
                    Ui.toast(act, "ذخیره شد");
                }
            }
        });
    }

    private void confirmVoid(final Models.License l) {
        new MaterialAlertDialogBuilder(act)
                .setTitle(l.voided ? "رفع ابطال" : "ابطال لایسنس")
                .setMessage(l.voided
                        ? "لایسنس دوباره در درآمد و پورسانت حساب شود؟"
                        : "لایسنس باطل شود؟ از درآمد، بدهی مشتری و پورسانت ویزیتور کم می‌شود (کد روی گوشی مشتری همچنان کار می‌کند؛ این فقط حساب‌وکتاب شماست).")
                .setPositiveButton("بله", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        Db.get(act).setLicenseVoid(l.id, !l.voided);
                        act.refreshAll();
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }
}
