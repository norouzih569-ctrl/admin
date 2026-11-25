package com.barber.app;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** صفحه‌های محصولات: فهرست، افزودن/ویرایش/حذف، فروش و فروش‌های ماه */
public final class ProductsUi {

    private ProductsUi() {
    }

    private static LinearLayout column(MainActivity act) {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 12), Ui.dp(act, 20), Ui.dp(act, 8));
        return box;
    }

    private static EditText field(MainActivity act, String hint, boolean numeric) {
        EditText e = new EditText(act);
        e.setHint(hint);
        if (numeric) e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private static final int C_OK = 0xFF1F9D84;
    private static final int C_LOW = 0xFFE08A00;
    private static final int C_OUT = 0xFFD9534F;

    private static TextView tv(MainActivity act, String text, float sp, int color, boolean bold) {
        TextView t = new TextView(act);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    /** دکمه‌ی گرد داخل کارت: پر (filled) یا کم‌رنگ */
    private static TextView pill(MainActivity act, String text, int color, boolean filled) {
        TextView t = tv(act, text, 14, filled ? 0xFFFFFFFF : color, true);
        t.setGravity(android.view.Gravity.CENTER);
        t.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 10));
        t.setBackground(filled ? Ui.tinted(act, color, 0xFF, 12) : Ui.tinted(act, color, 0x26, 12));
        return t;
    }

    private static View summaryBar(MainActivity act) {
        int[] t = JalaliCalendar.today();
        String[] r = JalaliCalendar.monthRangeIso(t[0], t[1]);
        List<Db.ProductSale> sales = Db.get(act).getProductSalesBetween(r[0], r[1]);
        double rev = 0, profit = 0;
        int cnt = 0;
        for (Db.ProductSale s : sales) {
            rev += s.total();
            profit += s.profit();
            cnt += s.qty;
        }
        int accent = Ui.themeColor(act, R.attr.barberAccent, C_OK);
        int sec = Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888);
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] labels = {"فروش این ماه", "سود این ماه", "تعداد فروخته‌شده"};
        String[] values = {Fmt.price(rev), Fmt.price(profit), String.valueOf(cnt)};
        for (int i = 0; i < 3; i++) {
            LinearLayout cell = new LinearLayout(act);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(Ui.dp(act, 10), Ui.dp(act, 8), Ui.dp(act, 10), Ui.dp(act, 8));
            cell.setBackground(Ui.tinted(act, accent, 0x1A, 12));
            cell.addView(tv(act, labels[i], 11, sec, false));
            cell.addView(tv(act, values[i], 14, accent, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(i == 0 ? 0 : Ui.dp(act, 6), 0, 0, 0);
            row.addView(cell, lp);
        }
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = Ui.dp(act, 12);
        row.setLayoutParams(rp);
        return row;
    }

    private static View productCard(final MainActivity act, final Db.Product p, final Runnable rebuild) {
        int accent = Ui.themeColor(act, R.attr.barberAccent, C_OK);
        int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        int sec = Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888);
        int st = p.stock <= 0 ? C_OUT : (p.stock <= 3 ? C_LOW : C_OK);
        String badge = p.stock <= 0 ? "❌ ناموجود" : (p.stock <= 3 ? "⚠ فقط " + p.stock + " عدد" : "موجودی " + p.stock);

        LinearLayout outer = new LinearLayout(act);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        outer.setBackground(Ui.tinted(act, prim, 0x0F, 16));
        outer.setClipToOutline(true);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        op.bottomMargin = Ui.dp(act, 10);
        outer.setLayoutParams(op);

        View strip = new View(act);
        strip.setBackgroundColor(st);
        outer.addView(strip, new LinearLayout.LayoutParams(Ui.dp(act, 5), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
        outer.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout top = new LinearLayout(act);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView name = tv(act, p.name, 16, prim, true);
        top.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView bd = tv(act, badge, 12, st, true);
        bd.setPadding(Ui.dp(act, 10), Ui.dp(act, 3), Ui.dp(act, 10), Ui.dp(act, 3));
        bd.setBackground(Ui.tinted(act, st, 0x26, 20));
        top.addView(bd);
        body.addView(top);

        TextView meta = tv(act, "خرید " + Fmt.price(p.buyPrice) + "  •  فروش " + Fmt.price(p.sellPrice), 12, sec, false);
        meta.setPadding(0, Ui.dp(act, 6), 0, Ui.dp(act, 4));
        body.addView(meta);

        double up = ProductMath.unitProfit(p.sellPrice, p.buyPrice);
        TextView chip = tv(act, "سود هر عدد " + Fmt.price(up) + " ("
                + Fmt.percent(Math.round(ProductMath.markupPercent(p.sellPrice, p.buyPrice))) + ")", 12, accent, true);
        chip.setPadding(Ui.dp(act, 8), Ui.dp(act, 2), Ui.dp(act, 8), Ui.dp(act, 2));
        chip.setBackground(Ui.tinted(act, accent, 0x1F, 10));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.gravity = android.view.Gravity.START;
        body.addView(chip, cp);

        LinearLayout btns = new LinearLayout(act);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = Ui.dp(act, 10);
        TextView bSell = pill(act, "💰 فروش", p.stock <= 0 ? 0xFF9AA5AB : accent, true);
        TextView bStock = pill(act, "＋ موجودی", accent, false);
        TextView bMore = pill(act, "⋯", sec, false);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f);
        LinearLayout.LayoutParams l3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f);
        l1.setMargins(0, 0, Ui.dp(act, 8), 0);
        l2.setMargins(0, 0, Ui.dp(act, 8), 0);
        btns.addView(bSell, l1);
        btns.addView(bStock, l2);
        btns.addView(bMore, l3);
        body.addView(btns, bp);

        bSell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sell(act, p, rebuild);
            }
        });
        bStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addStock(act, p, rebuild);
            }
        });
        View.OnClickListener more = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actions(act, p, rebuild);
            }
        };
        bMore.setOnClickListener(more);
        top.setOnClickListener(more);
        return outer;
    }

    /** فهرست محصولات */
    public static void show(final MainActivity act) {
        final LinearLayout box = column(act);
        ScrollView scroll = new ScrollView(act);
        scroll.addView(box);
        final AlertDialog d = new MaterialAlertDialogBuilder(act)
                .setTitle("🛍 محصولات و فروش")
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .setNeutralButton("+ محصول جدید", null)
                .setNegativeButton("فروش‌های ماه", null)
                .create();
        final Runnable[] rebuild = {null};
        rebuild[0] = new Runnable() {
            @Override
            public void run() {
                box.removeAllViews();
                List<Db.Product> list = Db.get(act).getProducts();
                if (list.isEmpty()) {
                    TextView note = new TextView(act);
                    note.setText("هنوز محصولی ثبت نشده. با «+ محصول جدید» شروع کنید (مثلاً ادکلن، ژل مو).");
                    note.setTextSize(13);
                    note.setPadding(0, 0, 0, Ui.dp(act, 8));
                    box.addView(note);
                    return;
                }
                box.addView(summaryBar(act));
                for (final Db.Product p : list) {
                    box.addView(productCard(act, p, rebuild[0]));
                }
            }
        };
        rebuild[0].run();
        d.show();
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit(act, null, rebuild[0]);
            }
        });
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monthSales(act, rebuild[0]);
            }
        });
    }

    private static TextView label(MainActivity act, String text) {
        TextView t = tv(act, text, 12, Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888), false);
        t.setPadding(Ui.dp(act, 4), Ui.dp(act, 12), 0, Ui.dp(act, 2));
        return t;
    }

    /** فیلد عددی با دکمه‌ی − و + دو طرفش */
    private static View stepper(final MainActivity act, final EditText et, final int min) {
        int accent = Ui.themeColor(act, R.attr.barberAccent, C_OK);
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView plus = tv(act, "+", 24, accent, true);
        TextView minus = tv(act, "−", 24, accent, true);
        TextView[] bs = {plus, minus};
        for (TextView b : bs) {
            b.setGravity(android.view.Gravity.CENTER);
            b.setBackground(Ui.tinted(act, accent, 0x26, 24));
        }
        et.setGravity(android.view.Gravity.CENTER);
        et.setTextSize(20);
        int sz = Ui.dp(act, 46);
        row.addView(plus, new LinearLayout.LayoutParams(sz, sz));
        row.addView(et, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(minus, new LinearLayout.LayoutParams(sz, sz));
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Double q = Fmt.parsePrice(et.getText().toString());
                int n = q == null ? min : (int) Math.round(q);
                et.setText(String.valueOf(Math.min(100000, n + 1)));
            }
        });
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Double q = Fmt.parsePrice(et.getText().toString());
                int n = q == null ? min : (int) Math.round(q);
                et.setText(String.valueOf(Math.max(min, n - 1)));
            }
        });
        return row;
    }

    private static LinearLayout cell(MainActivity act, String lbl, View v) {
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        c.addView(label(act, lbl));
        c.addView(v);
        return c;
    }

    private static void actions(final MainActivity act, final Db.Product p, final Runnable done) {
        new MaterialAlertDialogBuilder(act)
                .setTitle(p.name)
                .setItems(new CharSequence[]{"🛒 فروش", "✏️ ویرایش", "➕ افزودن موجودی", "حذف"},
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface x, int which) {
                                if (which == 0) sell(act, p, done);
                                else if (which == 1) edit(act, p, done);
                                else if (which == 2) addStock(act, p, done);
                                else confirmDelete(act, p, done);
                            }
                        })
                .show();
    }

    private static void edit(final MainActivity act, final Db.Product p, final Runnable done) {
        if (!act.canWrite()) return;
        LinearLayout box = column(act);
        final EditText etName = field(act, "نام محصول", false);
        final EditText etBuy = field(act, "قیمت خرید (" + AppConfig.CURRENCY + ")", true);
        final EditText etSell = field(act, "قیمت فروش (" + AppConfig.CURRENCY + ")", true);
        final EditText etStock = field(act, "موجودی (تعداد)", true);
        final TextView profit = new TextView(act);
        profit.setTextSize(13);
        if (p != null) {
            etName.setText(p.name);
            etBuy.setText(Fmt.plain(p.buyPrice));
            etSell.setText(Fmt.plain(p.sellPrice));
            etStock.setText(String.valueOf(p.stock));
        } else {
            etStock.setText("1");
        }
        android.text.TextWatcher tw = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                Double b = Fmt.parsePrice(etBuy.getText().toString());
                Double s = Fmt.parsePrice(etSell.getText().toString());
                if (b == null || s == null) {
                    profit.setText("");
                    profit.setVisibility(View.GONE);
                    return;
                }
                profit.setVisibility(View.VISIBLE);
                profit.setText("سود هر عدد: " + Fmt.money(ProductMath.unitProfit(s, b))
                        + " (" + Fmt.percent(Math.round(ProductMath.markupPercent(s, b))) + ")");
            }
        };
        etBuy.addTextChangedListener(tw);
        etSell.addTextChangedListener(tw);
        tw.afterTextChanged(null);
        etName.setHint("مثلاً ژل مو");
        etBuy.setHint("");
        etSell.setHint("");
        profit.setTextColor(Ui.themeColor(act, R.attr.barberAccent, C_OK));
        profit.setTypeface(profit.getTypeface(), android.graphics.Typeface.BOLD);
        profit.setTextSize(14);
        profit.setGravity(android.view.Gravity.CENTER);
        profit.setPadding(Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12));
        profit.setBackground(Ui.tinted(act, Ui.themeColor(act, R.attr.barberAccent, C_OK), 0x1F, 14));
        box.addView(label(act, "نام محصول"));
        box.addView(etName);
        LinearLayout prices = new LinearLayout(act);
        prices.setOrientation(LinearLayout.HORIZONTAL);
        prices.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams pr2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        pr2.setMargins(Ui.dp(act, 8), 0, 0, 0);
        prices.addView(cell(act, "قیمت خرید (" + AppConfig.CURRENCY + ")", etBuy), pl);
        prices.addView(cell(act, "قیمت فروش (" + AppConfig.CURRENCY + ")", etSell), pr2);
        box.addView(prices);
        box.addView(label(act, p == null ? "موجودی اولیه" : "موجودی (تعداد)"));
        box.addView(stepper(act, etStock, 0));
        LinearLayout.LayoutParams pfl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pfl.topMargin = Ui.dp(act, 16);
        box.addView(profit, pfl);
        final AlertDialog d = new MaterialAlertDialogBuilder(act)
                .setTitle(p == null ? "محصول جدید" : "ویرایش محصول")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = ServiceList.clean(etName.getText().toString());
                if (name.isEmpty()) {
                    etName.setError("نام محصول را وارد کنید");
                    return;
                }
                Double buy = Fmt.parsePrice(etBuy.getText().toString());
                if (buy == null) {
                    etBuy.setError("قیمت خرید صحیح نیست");
                    return;
                }
                Double sell = Fmt.parsePrice(etSell.getText().toString());
                if (sell == null) {
                    etSell.setError("قیمت فروش صحیح نیست");
                    return;
                }
                Double st = Fmt.parsePrice(etStock.getText().toString());
                if (st == null || st > 100000) {
                    etStock.setError("تعداد صحیح نیست");
                    return;
                }
                if (p == null) Db.get(act).addProduct(name, buy, sell, (int) Math.round(st));
                else Db.get(act).updateProduct(p.id, name, buy, sell, (int) Math.round(st));
                d.dismiss();
                done.run();
            }
        });
    }

    private static void addStock(final MainActivity act, final Db.Product p, final Runnable done) {
        if (!act.canWrite()) return;
        LinearLayout box = column(act);
        final EditText etQty = field(act, "", true);
        final EditText etBuy = field(act, "", true);
        etQty.setText("1");
        box.addView(label(act, "تعداد خرید جدید"));
        box.addView(stepper(act, etQty, 1));
        box.addView(label(act, "قیمت خرید این دسته (خالی = همان قبلی)"));
        box.addView(etBuy);
        final AlertDialog d = new MaterialAlertDialogBuilder(act)
                .setTitle("افزودن موجودی – " + p.name)
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Double q = Fmt.parsePrice(etQty.getText().toString());
                if (q == null || q < 1 || q > 100000) {
                    etQty.setError("تعداد صحیح نیست");
                    return;
                }
                double buy = p.buyPrice;
                if (!etBuy.getText().toString().trim().isEmpty()) {
                    Double b = Fmt.parsePrice(etBuy.getText().toString());
                    if (b == null) {
                        etBuy.setError("قیمت صحیح نیست");
                        return;
                    }
                    buy = b;
                }
                Db.get(act).updateProduct(p.id, p.name, buy, p.sellPrice, p.stock + (int) Math.round(q));
                d.dismiss();
                done.run();
            }
        });
    }

    private static void confirmDelete(final MainActivity act, final Db.Product p, final Runnable done) {
        if (!act.canWrite()) return;
        new MaterialAlertDialogBuilder(act)
                .setTitle("حذف «" + p.name + "»؟")
                .setMessage("محصول از لیست حذف می‌شود. فروش‌های قبلی در گزارش می‌مانند.")
                .setPositiveButton("حذف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface x, int w) {
                        Db.get(act).deleteProduct(p.id);
                        done.run();
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private static void sell(final MainActivity act, final Db.Product p, final Runnable done) {
        if (!act.canWrite()) return;
        if (p.stock <= 0) {
            Ui.toast(act, "موجودی این محصول صفر است");
            return;
        }
        final int accent = Ui.themeColor(act, R.attr.barberAccent, C_OK);
        final int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        LinearLayout box = column(act);

        // سربرگ محصول
        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
        head.setBackground(Ui.tinted(act, prim, 0x0F, 14));
        head.addView(tv(act, p.name, 16, prim, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        int st = p.stock <= 3 ? C_LOW : C_OK;
        TextView bd = tv(act, "موجودی " + p.stock, 12, st, true);
        bd.setPadding(Ui.dp(act, 10), Ui.dp(act, 3), Ui.dp(act, 10), Ui.dp(act, 3));
        bd.setBackground(Ui.tinted(act, st, 0x26, 20));
        head.addView(bd);
        box.addView(head);

        final EditText etQty = field(act, "", true);
        etQty.setText("1");
        final EditText etPrice = field(act, "", true);
        etPrice.setText(Fmt.plain(p.sellPrice));
        box.addView(label(act, "تعداد"));
        box.addView(stepper(act, etQty, 1));
        box.addView(label(act, "قیمت فروش هر عدد (" + AppConfig.CURRENCY + ")"));
        box.addView(etPrice);

        // روش پرداخت: دکمه‌های انتخابی
        box.addView(label(act, "پرداخت به‌صورت"));
        final int[] sel = {0};
        final TextView[] chips = new TextView[Pay.LABELS.length];
        LinearLayout payRow = new LinearLayout(act);
        payRow.setOrientation(LinearLayout.HORIZONTAL);
        payRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < chips.length; i++) {
                    boolean on = i == sel[0];
                    chips[i].setTextColor(on ? 0xFFFFFFFF : prim);
                    chips[i].setBackground(on ? Ui.tinted(act, accent, 0xFF, 12) : Ui.tinted(act, prim, 0x14, 12));
                }
            }
        };
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            TextView c = tv(act, Pay.LABELS[i], 14, prim, true);
            c.setGravity(android.view.Gravity.CENTER);
            c.setPadding(0, Ui.dp(act, 11), 0, Ui.dp(act, 11));
            c.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sel[0] = idx;
                    paint.run();
                }
            });
            chips[i] = c;
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) cl.setMargins(Ui.dp(act, 8), 0, 0, 0);
            payRow.addView(c, cl);
        }
        paint.run();
        box.addView(payRow);

        // جمع و سود
        final TextView info = tv(act, "", 13, 0xFFFFFFFF, false);
        info.setGravity(android.view.Gravity.CENTER);
        info.setPadding(Ui.dp(act, 12), Ui.dp(act, 14), Ui.dp(act, 12), Ui.dp(act, 14));
        info.setBackground(Ui.tinted(act, accent, 0xFF, 16));
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        il.topMargin = Ui.dp(act, 16);
        box.addView(info, il);
        android.text.TextWatcher tw = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                Double q = Fmt.parsePrice(etQty.getText().toString());
                Double pr = Fmt.parsePrice(etPrice.getText().toString());
                if (q == null || pr == null) {
                    info.setText("");
                    return;
                }
                info.setText("جمع فروش: " + Fmt.money(q * pr) + "\nسود شما: "
                        + Fmt.money(ProductMath.saleProfit((int) Math.round(q), pr, p.buyPrice)));
            }
        };
        etQty.addTextChangedListener(tw);
        etPrice.addTextChangedListener(tw);
        tw.afterTextChanged(null);
        final AlertDialog d = new MaterialAlertDialogBuilder(act)
                .setTitle("💰 ثبت فروش")
                .setView(Ui.scroll(box))
                .setPositiveButton("ثبت فروش", null)
                .setNegativeButton("انصراف", null)
                .create();
        d.show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Double q = Fmt.parsePrice(etQty.getText().toString());
                if (q == null) {
                    etQty.setError("تعداد صحیح نیست");
                    return;
                }
                String err = ProductMath.validateSale((int) Math.round(q), p.stock);
                if (err != null) {
                    etQty.setError(err);
                    return;
                }
                Double pr = Fmt.parsePrice(etPrice.getText().toString());
                if (pr == null) {
                    etPrice.setError("قیمت صحیح نیست");
                    return;
                }
                boolean ok = Db.get(act).sellProduct(p.id, (int) Math.round(q), pr, Pay.methodAt(sel[0]),
                        JalaliCalendar.todayGregorianString());
                d.dismiss();
                done.run();
                act.refreshAll();
                Ui.toast(act, ok ? "فروش ثبت شد" : "ثبت نشد");
            }
        });
    }

    /** فروش‌های ماه جاری؛ با لمس هر فروش می‌توان لغوش کرد (موجودی برمی‌گردد) */
    private static void monthSales(final MainActivity act, final Runnable refreshList) {
        int[] t = JalaliCalendar.today();
        String[] r = JalaliCalendar.monthRangeIso(t[0], t[1]);
        List<Db.ProductSale> sales = Db.get(act).getProductSalesBetween(r[0], r[1]);
        LinearLayout box = column(act);
        double rev = 0, profit = 0;
        for (final Db.ProductSale s : sales) {
            rev += s.total();
            profit += s.profit();
            TextView row = new TextView(act);
            row.setText(JalaliCalendar.gregorianStringToJalali(s.date) + "  •  " + s.name + " × " + s.qty
                    + "\n" + Fmt.money(s.total()) + "  •  سود " + Fmt.price(s.profit()) + "  •  " + Pay.label(s.method));
            row.setPadding(0, Ui.dp(act, 8), 0, Ui.dp(act, 8));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!act.canWrite()) return;
                    new MaterialAlertDialogBuilder(act)
                            .setTitle("لغو این فروش؟")
                            .setMessage(s.name + " × " + s.qty + " حذف می‌شود و موجودی برمی‌گردد.")
                            .setPositiveButton("لغو فروش", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface x, int w) {
                                    Db.get(act).deleteProductSale(s.id);
                                    refreshList.run();
                                    act.refreshAll();
                                    Ui.toast(act, "فروش لغو شد");
                                }
                            })
                            .setNegativeButton("انصراف", null)
                            .show();
                }
            });
            box.addView(row);
        }
        if (sales.isEmpty()) {
            TextView e = new TextView(act);
            e.setText("این ماه فروشی ثبت نشده است");
            box.addView(e);
        }
        ScrollView sv = new ScrollView(act);
        sv.addView(box);
        new MaterialAlertDialogBuilder(act)
                .setTitle("فروش‌های این ماه")
                .setMessage(sales.isEmpty() ? null : "جمع فروش: " + Fmt.money(rev) + "\nسود: " + Fmt.money(profit))
                .setView(sv)
                .setPositiveButton("بستن", null)
                .show();
    }
}
