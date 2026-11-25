package com.barber.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// ---- کلاس‌های کمکی کوچک (برای کم‌شدن تعداد فایل‌ها یک‌جا جمع شده‌اند) ----

/** هر تب برنامه باید بتواند اطلاعاتش را دوباره از دیتابیس بخواند. */
interface Refreshable {
    void refresh();
}

/** محاسبات خالص فروش محصول (قابل تست) */
final class ProductMath {
    private ProductMath() {
    }

    /** سود هر واحد */
    public static double unitProfit(double sell, double buy) {
        return sell - buy;
    }

    /** درصد سود نسبت به قیمت خرید؛ اگر خرید صفر باشد 0 */
    public static double markupPercent(double sell, double buy) {
        return buy <= 0 ? 0 : (sell - buy) * 100.0 / buy;
    }

    public static double saleProfit(int qty, double unitPrice, double unitCost) {
        return qty * (unitPrice - unitCost);
    }

    /** null اگر درست است، وگرنه پیام خطا */
    public static String validateSale(int qty, int stock) {
        if (qty <= 0) return "تعداد باید حداقل ۱ باشد";
        if (qty > stock) return "موجودی کافی نیست (موجودی: " + stock + ")";
        return null;
    }
}

/** محاسبات خالص نمودار و مقایسه‌ی گزارش (قابل تست) */
final class ReportMath {
    private ReportMath() {
    }

    /** جمع مبلغ هر روز ماه؛ days[i] شماره‌ی روز (۱ تا monthLen) */
    public static double[] dailyTotals(int[] days, double[] amounts, int monthLen) {
        double[] out = new double[monthLen];
        for (int i = 0; i < days.length; i++) {
            int d = days[i];
            if (d >= 1 && d <= monthLen) out[d - 1] += amounts[i];
        }
        return out;
    }

    /** درصد تغییر نسبت به ماه قبل؛ null اگر ماه قبل صفر بود (مقایسه معنی ندارد) */
    public static Double changePercent(double cur, double prev) {
        if (prev <= 0) return null;
        return (cur - prev) * 100.0 / prev;
    }

    /** اندیس بیشترین مقدار؛ -1 اگر همه صفر */
    public static int maxIndex(double[] v) {
        int best = -1;
        double m = 0;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > m) {
                m = v[i];
                best = i;
            }
        }
        return best;
    }

    public static String describeChange(double cur, double prev) {
        Double p = changePercent(cur, prev);
        if (p == null) return cur > 0 ? "ماه قبل درآمدی ثبت نشده بود" : "";
        long r = Math.round(Math.abs(p));
        if (r == 0) return "تقریباً برابر با ماه قبل";
        return (p > 0 ? "▲ " : "▼ ") + r + "٪ " + (p > 0 ? "بیشتر" : "کمتر") + " از ماه قبل";
    }
}

/** منطق خالص سقف سالانه‌ی درآمد کارتی/واریزی (قابل تست) */
final class TaxCap {
    public static final int OK = 0, WARN = 1, OVER = 2;

    private TaxCap() {
    }

    /** cap <= 0 یعنی غیرفعال */
    public static int level(double received, double cap, int warnPercent) {
        if (cap <= 0) return OK;
        if (received >= cap) return OVER;
        if (received >= cap * warnPercent / 100.0) return WARN;
        return OK;
    }

    public static double percentUsed(double received, double cap) {
        return cap <= 0 ? 0 : received * 100.0 / cap;
    }
}

/** منطق خالص لیست خدمات (بدون وابستگی به اندروید؛ قابل تست) */
final class ServiceList {

    public static final int MAX_NAME = 30;
    public static final int MAX_COUNT = 40;

    private ServiceList() {
    }

    public static String join(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(n);
        }
        return sb.toString();
    }

    public static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw.split("\n")) {
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** نام تمیز: فاصله‌های اضافه و خط جدید حذف می‌شود */
    public static String clean(String name) {
        if (name == null) return "";
        return name.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    /**
     * @param self نام فعلی هنگام ویرایش (یا null هنگام افزودن)
     * @return متن خطا یا null اگر درست است
     */
    public static String validateName(String name, List<String> existing, String self) {
        String n = clean(name);
        if (n.isEmpty()) return "نام خدمت را وارد کنید";
        if (n.length() > MAX_NAME) return "نام خدمت حداکثر " + MAX_NAME + " حرف";
        if (n.indexOf('+') >= 0 || n.indexOf('|') >= 0) return "از + و | در نام استفاده نکنید";
        for (String e : existing) {
            if (e.equals(n) && (self == null || !e.equals(self))) return "این خدمت قبلاً وجود دارد";
        }
        if (self == null && existing.size() >= MAX_COUNT) return "حداکثر " + MAX_COUNT + " خدمت";
        return null;
    }
}

/** روش‌های پرداخت و محاسبه‌ی بدهی (منطق خالص؛ قابل تست) */
final class Pay {

    public static final String CASH = "cash";
    public static final String CARD = "card";
    public static final String TRANSFER = "transfer";

    public static final String[] METHODS = {CASH, CARD, TRANSFER};
    public static final String[] LABELS = {"💵 نقد", "💳 کارت", "🏦 واریز"};

    private Pay() {
    }

    public static String label(String method) {
        if ("pkg".equals(method)) return "📦 پکیج";
        for (int i = 0; i < METHODS.length; i++) {
            if (METHODS[i].equals(method)) return LABELS[i];
        }
        return "—";
    }

    public static String methodAt(int index) {
        return (index >= 0 && index < METHODS.length) ? METHODS[index] : CASH;
    }

    /**
     * بدهی یک نوبت. نوبت قدیمی (tracked=false) یا در انتظار، بدهی ندارد.
     * @param earned مبلغ حساب‌شده‌ی نوبت (final_price یا price)
     */
    public static double debt(boolean tracked, boolean done, double earned, double paid) {
        if (!tracked || !done) return 0;
        double d = earned - paid;
        return d > 0.5 ? d : 0; // کمتر از نیم تومان = تسویه
    }

    /**
     * مبلغ دریافتی را از متن فیلد می‌خواند. خالی = کل مبلغ.
     * @return null اگر نامعتبر باشد (منفی یا بیشتر از کل)
     */
    public static Double resolvePaid(String fieldText, double total) {
        String t = fieldText == null ? "" : fieldText.trim();
        if (t.isEmpty()) return total;
        Double v = Fmt.parsePrice(t);
        if (v == null || v < 0 || v > total) return null;
        return v;
    }
}

/** ارسال دستی پیامک با برنامه‌ی پیامک گوشی (بدون نیاز به مجوز؛ خود کاربر دکمه‌ی ارسال را می‌زند). */
final class Messenger {

    private Messenger() {
    }

    /** پیج اینستاگرام را باز می‌کند (اول برنامه‌ی اینستاگرام، وگرنه مرورگر). */
    public static void openInstagram(Activity act, String id) {
        String u = AppConfig.cleanInstagram(id);
        if (u.isEmpty()) {
            Ui.toastLong(act, "آیدی اینستاگرام ثبت نشده است");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/_u/" + u));
            i.setPackage("com.instagram.android");
            act.startActivity(i);
        } catch (Exception e) {
            try {
                act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/" + u)));
            } catch (Exception e2) {
                Ui.toastLong(act, "برنامه‌ی اینستاگرام یا مرورگر پیدا نشد");
            }
        }
    }

    public static void openSms(Activity act, String phone, String text) {
        String d = Logic.digitsOnly(phone);
        if (d.isEmpty()) {
            Ui.toastLong(act, "شماره‌ی تلفن مشتری ثبت نشده است");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + d));
            i.putExtra("sms_body", text);
            act.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Ui.toastLong(act, "برنامه‌ی پیامک روی گوشی پیدا نشد");
        }
    }
}

/** منطق خالص یادآورهای پرداخت (اجاره، قسط، خرید): تاریخ بعدی و روزهای مانده */
final class ReminderMath {
    private ReminderMath() {
    }

    /** سررسید ماه شمسی بعد، با همان روز (اگر ماه بعد آن روز را ندارد، آخرین روز ماه) */
    public static String nextMonthDue(String dueIso, int anchorDay) {
        int[] j = JalaliCalendar.jalaliOfIso(dueIso);
        int y = j[0], m = j[1] + 1;
        if (m > 12) {
            m = 1;
            y++;
        }
        int d = Math.min(anchorDay > 0 ? anchorDay : j[2], JalaliCalendar.monthLength(y, m));
        return JalaliCalendar.jalaliToGregorianString(y, m, d);
    }

    /** مثبت = روز مانده؛ ۰ = امروز؛ منفی = روز گذشته از سررسید */
    public static long daysLeft(String dueIso, String todayIso) {
        return ChronoUnit.DAYS.between(LocalDate.parse(todayIso), LocalDate.parse(dueIso));
    }

    /** آیا باید در برنامه هشدار داد؟ (گذشته یا در بازه‌ی leadDays روز مانده) */
    public static boolean isDueSoon(String dueIso, String todayIso, int leadDays) {
        return daysLeft(dueIso, todayIso) <= leadDays;
    }

    public static String describePlain(long days) {
        if (days < 0) return (-days) + " روز گذشته";
        if (days == 0) return "امروز";
        if (days == 1) return "فردا";
        return days + " روز دیگر";
    }

    public static String describe(long days) {
        String icon = days < 0 ? "🔴 " : days == 0 ? "🟠 " : days == 1 ? "🟡 " : "";
        return icon + describePlain(days);
    }
}

/**
 * کلید مخفی سازنده/بررسی‌کننده‌ی کد لایسنس.
 * ⚠️ دقیقاً همین مقدار باید در «برنامه‌ی مدیریتی» هم باشد، وگرنه کدها قبول نمی‌شوند.
 * مخزن (Repository) GitHub را حتماً خصوصی (Private) نگه دارید و این مقدار را جایی منتشر نکنید.
 */
final class LicenseKeys {
    static final String SECRET = "5b0752dcd05010fcfe835076876430e76784702ba4b45b224c5abf1585e1a1e6";

    private LicenseKeys() {
    }
}

/** عکس مدل مو در پروفایل مشتری: دوربین یا گالری، کوچک‌سازی، ذخیره در فایل جدا */
final class PhotoUi {

    /** مشتری‌ای که عکسش در حال گرفتن/انتخاب است (بین بازگشت از دوربین/گالری نگه داشته می‌شود) */
    static long pendingCustomer = -1;
    static java.io.File pendingCamera;

    private static final int MAX_SIDE = 1024;

    private PhotoUi() {
    }

    interface Opener {
        java.io.InputStream open() throws java.io.IOException;
    }

    static boolean canAdd(MainActivity act, long customerId) {
        if (!act.canWrite()) return false;
        if (Db.get(act).getCustomerPhotos(customerId).size() >= Db.MAX_PHOTOS) {
            Ui.toastLong(act, "حداکثر " + Db.MAX_PHOTOS + " عکس برای هر مشتری. اول یکی را حذف کنید.");
            return false;
        }
        return true;
    }

    static void startCamera(MainActivity act, long customerId) {
        if (!canAdd(act, customerId)) return;
        try {
            java.io.File dir = new java.io.File(act.getCacheDir(), "camera");
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, "tmp.jpg");
            pendingCustomer = customerId;
            pendingCamera = f;
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    act, act.getPackageName() + ".fileprovider", f);
            act.cameraLauncher.launch(uri);
        } catch (Exception e) {
            Ui.toastLong(act, "دوربین باز نشد");
        }
    }

    static void startGallery(MainActivity act, long customerId) {
        if (!canAdd(act, customerId)) return;
        pendingCustomer = customerId;
        try {
            act.galleryLauncher.launch("image/*");
        } catch (Exception e) {
            Ui.toastLong(act, "گالری باز نشد");
        }
    }

    static void onCameraResult(final MainActivity act, boolean ok) {
        final java.io.File f = pendingCamera;
        if (!ok || f == null || !f.exists()) {
            reopen(act);
            return;
        }
        process(act, new Opener() {
            @Override
            public java.io.InputStream open() throws java.io.IOException {
                return new java.io.FileInputStream(f);
            }
        }, f);
    }

    static void onGalleryResult(final MainActivity act, final android.net.Uri uri) {
        if (uri == null) {
            reopen(act);
            return;
        }
        process(act, new Opener() {
            @Override
            public java.io.InputStream open() throws java.io.IOException {
                java.io.InputStream in = act.getContentResolver().openInputStream(uri);
                if (in == null) throw new java.io.IOException("no stream");
                return in;
            }
        }, null);
    }

    private static void reopen(MainActivity act) {
        if (pendingCustomer >= 0) CustomerProfile.show(act, pendingCustomer);
    }

    /** خواندن، چرخاندن طبق EXIF، کوچک‌سازی تا ۱۰۲۴ پیکسل و ذخیره (در رشته‌ی پس‌زمینه) */
    private static void process(final MainActivity act, final Opener src, final java.io.File tempToDelete) {
        final long cid = pendingCustomer;
        Ui.toast(act, "در حال ذخیره‌ی عکس…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String saved = null;
                try {
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    java.io.InputStream in = src.open();
                    try {
                        android.graphics.BitmapFactory.decodeStream(in, null, o);
                    } finally {
                        in.close();
                    }
                    int sample = 1;
                    while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
                    android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
                    o2.inSampleSize = sample;
                    android.graphics.Bitmap bmp;
                    in = src.open();
                    try {
                        bmp = android.graphics.BitmapFactory.decodeStream(in, null, o2);
                    } finally {
                        in.close();
                    }
                    if (bmp == null) throw new java.io.IOException("decode");
                    int rot = 0;
                    try {
                        in = src.open();
                        try {
                            int ori = new android.media.ExifInterface(in).getAttributeInt(
                                    android.media.ExifInterface.TAG_ORIENTATION,
                                    android.media.ExifInterface.ORIENTATION_NORMAL);
                            if (ori == android.media.ExifInterface.ORIENTATION_ROTATE_90) rot = 90;
                            else if (ori == android.media.ExifInterface.ORIENTATION_ROTATE_180) rot = 180;
                            else if (ori == android.media.ExifInterface.ORIENTATION_ROTATE_270) rot = 270;
                        } finally {
                            in.close();
                        }
                    } catch (Exception ignored) {
                        // بدون EXIF: چرخش نمی‌دهیم
                    }
                    float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.postRotate(rot);
                    m.postScale(scale, scale);
                    android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(
                            bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                    String name = "p" + cid + "_" + System.currentTimeMillis() + ".jpg";
                    java.io.File dest = Db.get(act).photoFile(name);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
                    try {
                        out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, fos);
                    } finally {
                        fos.close();
                    }
                    long id = Db.get(act).addCustomerPhoto(cid, name,
                            JalaliCalendar.todayGregorianString(), "");
                    if (id < 0) {
                        dest.delete();
                    } else {
                        saved = name;
                    }
                } catch (Throwable t) {
                    saved = null;
                }
                if (tempToDelete != null) tempToDelete.delete();
                final boolean ok = saved != null;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Ui.toast(act, ok ? "عکس ذخیره شد ✓" : "ذخیره‌ی عکس ناموفق بود");
                        reopen(act);
                    }
                });
            }
        }).start();
    }

    /** بندانگشتی: نسخه‌ی کوچک‌شده‌ی فایل (برای لیست) */
    static android.graphics.Bitmap thumb(java.io.File f, int target) {
        try {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= target) s *= 2;
            android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
            o2.inSampleSize = s;
            return android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    /** نمایش بزرگ یک عکس با امکان حذف؛ با بستن، دوباره پروفایل باز می‌شود */
    static void viewer(final MainActivity act, final Db.CustomerPhoto p) {
        final long cid = p.customerId;
        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 8), Ui.dp(act, 8), Ui.dp(act, 8), Ui.dp(act, 4));
        android.widget.ImageView iv = new android.widget.ImageView(act);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        android.graphics.Bitmap b = thumb(Db.get(act).photoFile(p.file), 1024);
        if (b != null) {
            iv.setImageBitmap(b);
        } else {
            iv.setImageDrawable(null);
        }
        box.addView(iv, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        android.widget.TextView cap = new android.widget.TextView(act);
        cap.setText("📅 " + JalaliCalendar.gregorianStringToJalali(p.taken)
                + (p.note == null || p.note.isEmpty() ? "" : "\n📝 " + p.note)
                + (b == null ? "\n⚠️ فایل این عکس در دسترس نیست (احتمالاً در پشتیبان نبوده)" : ""));
        cap.setPadding(0, Ui.dp(act, 6), 0, 0);
        box.addView(cap);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("📷 عکس مدل مو")
                .setView(Ui.scroll(box))
                .setPositiveButton("بستن", null)
                .setNeutralButton("🗑 حذف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        confirmDelete(act, p);
                    }
                })
                .setNegativeButton("✏️ تاریخ و توضیح", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        editMeta(act, p);
                    }
                })
                .setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(android.content.DialogInterface d) {
                        if (!deleting) CustomerProfile.show(act, cid);
                        deleting = false;
                    }
                })
                .show();
    }

    private static boolean deleting = false;

    /** ویرایش تاریخ و توضیح (مثلاً «فید ۲ از کنار، ریش خط‌دار») */
    private static void editMeta(final MainActivity act, final Db.CustomerPhoto p) {
        if (!act.canWrite()) {
            viewer(act, p);
            return;
        }
        deleting = true; // با بسته شدن پنجره‌ی عکس، پروفایل باز نشود؛ خودمان دوباره پنجره‌ی عکس را باز می‌کنیم
        final String[] date = {p.taken};
        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 8), Ui.dp(act, 20), 0);
        final android.widget.Button bd = new android.widget.Button(act);
        bd.setText("تاریخ: " + JalaliCalendar.gregorianStringToJalali(date[0]));
        bd.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                JalaliPicker.show(act, JalaliCalendar.jalaliOfIso(date[0]), new JalaliPicker.OnPicked() {
                    @Override
                    public void onPicked(int y, int m, int d) {
                        date[0] = JalaliCalendar.jalaliToGregorianString(y, m, d);
                        bd.setText("تاریخ: " + JalaliCalendar.format(y, m, d));
                    }
                });
            }
        });
        box.addView(bd);
        final android.widget.EditText et = new android.widget.EditText(act);
        et.setHint("توضیح کوتاه (مثلاً فید ۲ از کنار)");
        et.setText(p.note);
        et.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        box.addView(et);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("تاریخ و توضیح عکس")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Db db = Db.get(act);
                        db.updateCustomerPhoto(p.id, date[0], et.getText().toString());
                        deleting = false;
                        Db.CustomerPhoto fresh = db.getCustomerPhoto(p.id);
                        viewer(act, fresh == null ? p : fresh);
                    }
                })
                .setNegativeButton("انصراف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        deleting = false;
                        viewer(act, p);
                    }
                })
                .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(android.content.DialogInterface d) {
                        deleting = false;
                        viewer(act, p);
                    }
                })
                .show();
    }

    static String mb(long bytes) {
        return String.format(java.util.Locale.US, "%.1f", bytes / 1048576.0);
    }

    /** پشتیبان‌گیری: انتخاب اینکه عکس‌ها هم شامل شوند یا نه (پیش‌فرض خاموش) */
    static void backupDialog(final MainActivity act) {
        long[] r = Db.get(act).photoScan(false);
        long usedFiles = r[3] - r[0];
        long usedBytes = r[4] - r[1];
        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 8), Ui.dp(act, 20), 0);
        final android.widget.CheckBox cb = new android.widget.CheckBox(act);
        cb.setText("عکس‌های مدل مو هم در پشتیبان باشد");
        cb.setChecked(AppConfig.getBackupPhotos(act) && usedFiles > 0);
        cb.setEnabled(usedFiles > 0);
        box.addView(cb);
        android.widget.TextView info = new android.widget.TextView(act);
        info.setTextSize(13);
        info.setText(usedFiles > 0
                ? "عکس‌های ذخیره‌شده: " + usedFiles + " فایل، حدود " + mb(usedBytes)
                + " مگابایت. با روشن بودن این گزینه، فایل پشتیبان به‌صورت zip و بزرگ‌تر ذخیره می‌شود. "
                + "این فایل شامل عکس مشتری‌هاست؛ آن را فقط جای امن نگه دارید."
                : "هنوز عکسی ذخیره نکرده‌اید؛ پشتیبان فقط اطلاعات برنامه را دارد.");
        info.setPadding(0, Ui.dp(act, 8), 0, 0);
        box.addView(info);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("💾 پشتیبان‌گیری")
                .setView(Ui.scroll(box))
                .setPositiveButton("پشتیبان‌گیری", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        boolean with = cb.isEnabled() && cb.isChecked();
                        if (cb.isEnabled()) AppConfig.setBackupPhotos(act, with);
                        act.startBackup(with);
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    /** پاک‌سازی: فایل‌های عکسِ بی‌صاحب و ردیف‌های عکسی که فایلش نیست */
    static void cleanupDialog(final MainActivity act) {
        long[] r = Db.get(act).photoScan(false);
        String msg = "عکس‌های ذخیره‌شده: " + r[3] + " فایل، " + mb(r[4]) + " مگابایت\n\n"
                + "فایل بی‌صاحب (به هیچ مشتری وصل نیست): " + r[0] + " (" + mb(r[1]) + " مگابایت)\n"
                + "ردیف عکس بدون فایل (مثلاً بعد از بازیابی پشتیبان بدون عکس): " + r[2];
        com.google.android.material.dialog.MaterialAlertDialogBuilder b =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                        .setTitle("🧹 پاک‌سازی عکس‌ها")
                        .setMessage(msg)
                        .setNegativeButton("بستن", null);
        if (r[0] + r[2] > 0) {
            b.setPositiveButton("پاک‌سازی", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int w) {
                    if (!act.canWrite()) return;
                    long[] x = Db.get(act).photoScan(true);
                    Ui.toast(act, "پاک شد: " + x[0] + " فایل و " + x[2] + " ردیف");
                }
            });
        } else {
            b.setMessage(msg + "\n\n✓ چیزی برای پاک‌سازی نیست.");
        }
        b.show();
    }

    private static void confirmDelete(final MainActivity act, final Db.CustomerPhoto p) {
        if (!act.canWrite()) {
            return;
        }
        deleting = true;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("حذف عکس")
                .setMessage("این عکس برای همیشه حذف شود؟")
                .setPositiveButton("حذف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Db.get(act).deleteCustomerPhoto(p.id);
                        deleting = false;
                        CustomerProfile.show(act, p.customerId);
                    }
                })
                .setNegativeButton("انصراف", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        deleting = false;
                        viewer(act, p);
                    }
                })
                .setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(android.content.DialogInterface d) {
                        deleting = false;
                        viewer(act, p);
                    }
                })
                .show();
    }
}


/** تنظیمات نوبت‌گیری پیامکی (فقط نسخه‌ی booking): روشن/خاموش، کلمه‌ی شروع، حالت ثبت. */
final class BookingUi {

    static final int REQ_BOOKING = 7731;

    /** وقتی فهرست درخواست‌ها عوض شد، صفحه‌ی باز (اگر باشد) شمارنده را به‌روز می‌کند */
    static volatile Runnable onChanged;

    static void fireChanged() {
        final Runnable r = onChanged;
        if (r != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    // ---------------- دریافت پیامک و ارسال پاسخ (مرحله ۳) ----------------

    /** گیرنده‌ی پیامک ورودی؛ فقط در manifest نسخه‌ی booking ثبت می‌شود. */
    public static class SmsReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context ctx, android.content.Intent intent) {
            try {
                if (!AppConfig.getBookingOn(ctx)) return;
                if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
                android.telephony.SmsMessage[] msgs = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);
                if (msgs == null || msgs.length == 0) return;
                StringBuilder sb = new StringBuilder();
                for (android.telephony.SmsMessage m : msgs) {
                    if (m != null && m.getMessageBody() != null) sb.append(m.getMessageBody());
                }
                int sub = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX",
                        intent.getIntExtra("subscription", -1));
                handle(ctx, msgs[0].getOriginatingAddress(), sb.toString(), sub);
            } catch (Exception ignored) {
                // هر خطایی نباید باعث بسته‌شدن برنامه شود
            }
        }
    }

    /** ارقام فارسی/عربی→لاتین، ی/ک عربی→فارسی، حذف نیم‌فاصله، فاصله‌های اضافی */
    static String normalize(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') c = (char) ('0' + (c - '\u06F0'));
            else if (c >= '\u0660' && c <= '\u0669') c = (char) ('0' + (c - '\u0660'));
            else if (c == '\u064A') c = '\u06CC';
            else if (c == '\u0643') c = '\u06A9';
            else if (c == '\u200C' || c == '\u200F' || c == '\u200E') continue;
            b.append(c);
        }
        return b.toString().trim().replaceAll("\\s+", " ");
    }

    /** شماره‌ی موبایل ایران به شکل 09xxxxxxxxx؛ برای هر چیز دیگر (سرشماره، تبلیغاتی) خالی */
    static String normPhone(String raw) {
        if (raw == null) return "";
        String d = normalize(raw).replaceAll("[^0-9+]", "");
        if (d.startsWith("+98")) d = "0" + d.substring(3);
        else if (d.startsWith("0098")) d = "0" + d.substring(4);
        else if (d.startsWith("98") && d.length() == 12) d = "0" + d.substring(2);
        return d.matches("09\\d{9}") ? d : "";
    }

    /** آیا پیامک با کلمه‌ی شروع شروع می‌شود؟ */
    static boolean isStart(android.content.Context ctx, String text) {
        String key = normalize(AppConfig.getBookingKeyword(ctx));
        String t = normalize(text);
        return t.equals(key) || t.startsWith(key + " ");
    }

    // ---------------- گفت‌وگوی نوبت‌گیری (مرحله ۴: خدمت و روز) ----------------

    private static final long SESSION_MS = 30L * 60L * 1000L;
    private static final int DAYS = 5;
    private static final int MAX_SERVICES = 9;

    private static android.content.SharedPreferences sessions(android.content.Context ctx) {
        return ctx.getSharedPreferences("booking_sessions", android.content.Context.MODE_PRIVATE);
    }

    private static org.json.JSONObject loadSession(android.content.Context ctx, String phone) {
        try {
            String raw = sessions(ctx).getString(phone, null);
            return raw == null ? null : new org.json.JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveSession(android.content.Context ctx, String phone, org.json.JSONObject o) {
        try {
            o.put("ts", System.currentTimeMillis());
            sessions(ctx).edit().putString(phone, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static void clearSession(android.content.Context ctx, String phone) {
        sessions(ctx).edit().remove(phone).apply();
    }

    /** خدماتی که مشتری می‌تواند با پیامک انتخاب کند (حداکثر ۹ تا تا شماره‌ها یک‌رقمی بماند) */
    static java.util.List<String> bookableServices(android.content.Context ctx) {
        java.util.List<String> all = AppConfig.getServices(ctx);
        return new java.util.ArrayList<>(all.subList(0, Math.min(all.size(), MAX_SERVICES)));
    }

    /** تاریخ میلادی (ISO) روزِ i ام از امروز */
    static String dayIso(int offset) {
        int[] t = JalaliCalendar.today();
        int[] d = JalaliCalendar.addDays(t[0], t[1], t[2], offset);
        return JalaliCalendar.jalaliToGregorianString(d[0], d[1], d[2]);
    }

    /** مدت و قیمت یک یا چند خدمت (جداشده با +) */
    static int svcDuration(android.content.Context ctx, String service) {
        int sum = 0;
        for (String nm : Logic.splitServices(service)) sum += Math.max(10, AppConfig.getDuration(ctx, nm));
        return Math.max(10, sum);
    }

    static double svcPrice(android.content.Context ctx, String service) {
        double sum = 0;
        for (String nm : Logic.splitServices(service)) sum += AppConfig.getPrice(ctx, nm);
        return sum;
    }

    /** «1+3»، «1 3»، «1,3» یا «1 و 3» → [1, 3]؛ اگر شکل پیام درست نبود null */
    static java.util.List<Integer> parseNums(String text) {
        String t = normalize(text).replace("و", "+").replace("،", "+").replace(",", "+").replace(" ", "+");
        if (!t.matches("\\d{1,2}(\\+{1,3}\\d{1,2}){0,3}")) return null;
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (String part : t.split("\\+")) {
            if (!part.isEmpty()) out.add(Integer.parseInt(part));
        }
        return out;
    }

    private static String serviceMenu(android.content.Context ctx) {
        java.util.List<String> l = bookableServices(ctx);
        StringBuilder sb = new StringBuilder("سلام! " + AppConfig.getSalonName(ctx)
                + "\nخدمت را با شماره انتخاب کنید:");
        for (int i = 0; i < l.size(); i++) {
            long pr = AppConfig.getPrice(ctx, l.get(i));
            sb.append("\n").append(i + 1).append(" ").append(l.get(i));
            if (pr > 0) sb.append(" (").append(Fmt.price(pr)).append(")");
        }
        sb.append("\nچند خدمت: مثل 1+3\nانصراف: لغو");
        return sb.toString();
    }

    /** ۵ روز بازِ بعدی (روزهای تعطیل حذف می‌شوند)؛ حداکثر ۶۰ روز جلو را نگاه می‌کند */
    static java.util.List<String> openDays(android.content.Context ctx) {
        Db db = Db.get(ctx);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (int off = 0; off < 60 && out.size() < DAYS; off++) {
            String iso = dayIso(off);
            if (db.closedDay(iso) == null) out.add(iso);
        }
        return out;
    }

    private static String dayMenu(java.util.List<String> days) {
        StringBuilder sb = new StringBuilder("روز را انتخاب کنید:");
        String today = dayIso(0);
        String tomorrow = dayIso(1);
        for (int i = 0; i < days.size(); i++) {
            String iso = days.get(i);
            sb.append("\n").append(i + 1).append(" ")
                    .append(iso.equals(today) ? "امروز " : (iso.equals(tomorrow) ? "فردا " : ""))
                    .append(JalaliCalendar.gregorianStringToJalali(iso));
        }
        return sb.toString();
    }

    private static void startFlow(android.content.Context ctx, String phone) {
        if (upcomingFor(ctx, phone).size() + pendingFor(ctx, phone) >= MAX_ACTIVE_PER_PHONE) {
            sendSms(ctx, phone, "شما چند نوبت فعال دارید. برای لغو یکی «لغو نوبت» را بفرستید.");
            return;
        }
        try {
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("step", "service");
            saveSession(ctx, phone, o);
        } catch (Exception e) {
            return;
        }
        sendSms(ctx, phone, serviceMenu(ctx));
    }

    // ---------------- اعلان به آرایشگر (مرحله ۸) ----------------

    private static final String BOOKING_CHANNEL = "booking_requests";

    /** اعلان برای صاحب آرایشگاه؛ با لمس آن برنامه باز می‌شود. */
    static void notifyOwner(android.content.Context ctx, String title, String text) {
        notifyOwner(ctx, title, text, false);
    }

    /** openRequests=true: لمس اعلان مستقیم صفحه‌ی درخواست‌ها را باز می‌کند */
    static void notifyOwner(android.content.Context ctx, String title, String text, boolean openRequests) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager)
                    ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new android.app.NotificationChannel(
                        BOOKING_CHANNEL, "نوبت‌گیری پیامکی", android.app.NotificationManager.IMPORTANCE_HIGH));
            }
            android.content.Intent open = new android.content.Intent(ctx, MainActivity.class);
            open.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (openRequests) open.putExtra("open_booking_requests", true);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(ctx, openRequests ? 8 : 7, open,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26
                    ? new android.app.Notification.Builder(ctx, BOOKING_CHANNEL)
                    : new android.app.Notification.Builder(ctx);
            b.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            nm.notify((int) (System.currentTimeMillis() % 1000000L) + 3000000, b.build());
        } catch (Exception ignored) {
        }
    }

    private static String who(android.content.Context ctx, String phone) {
        String nm = customerNameByPhone(ctx, phone);
        return nm.isEmpty() ? phone : nm + " (" + phone + ")";
    }

    // ---------------- وضعیت باتری ----------------

    static boolean batteryUnrestricted(android.content.Context ctx) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(android.content.Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    static void openBatterySettings(MainActivity act) {
        try {
            act.startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:" + act.getPackageName())));
        } catch (Exception e) {
            try {
                act.startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {
                Ui.toast(act, "تنظیمات باتری باز نشد؛ از تنظیمات گوشی «برنامه‌ها > مدیریت باتری» را تنظیم کنید");
            }
        }
    }

    // ---------------- درخواست‌های منتظر تأیید (مرحله ۶) ----------------

    private static org.json.JSONArray loadPending(android.content.Context ctx) {
        try {
            return new org.json.JSONArray(ctx.getSharedPreferences("booking_pending",
                    android.content.Context.MODE_PRIVATE).getString("list", "[]"));
        } catch (Exception e) {
            return new org.json.JSONArray();
        }
    }

    static int pendingCount(android.content.Context ctx) {
        return loadPending(ctx).length();
    }

    private static void removePending(android.content.Context ctx, long ts) {
        try {
            org.json.JSONArray a = loadPending(ctx);
            org.json.JSONArray keep = new org.json.JSONArray();
            for (int i = 0; i < a.length(); i++) {
                if (a.getJSONObject(i).optLong("ts") != ts) keep.put(a.getJSONObject(i));
            }
            ctx.getSharedPreferences("booking_pending", android.content.Context.MODE_PRIVATE)
                    .edit().putString("list", keep.toString()).apply();
            fireChanged();
        } catch (Exception ignored) {
        }
    }

    private static String customerNameByPhone(android.content.Context ctx, String phone) {
        for (Db.Customer c : Db.get(ctx).getCustomers()) {
            if (phone.equals(normPhone(c.phone))) return c.name;
        }
        return "";
    }

    /** تأیید یک درخواست: نوبت ثبت و پیامک تأیید ارسال می‌شود. false اگر ساعت دیگر خالی نبود. */
    private static boolean approve(MainActivity act, org.json.JSONObject o) {
        String phone = o.optString("phone");
        String service = o.optString("service");
        String iso = o.optString("date");
        String time = o.optString("time");
        int dur = svcDuration(act, service);
        Db db = Db.get(act);
        if (db.findConflict(iso, time, dur, -1) != null) {
            Ui.toastLong(act, "این ساعت دیگر خالی نیست؛ درخواست را رد کنید یا نوبت دیگری را جابه‌جا کنید");
            return false;
        }
        long cid = findOrCreateCustomer(act, phone);
        long id = cid < 0 ? -1 : db.addAppointment(cid, iso, time, service,
                svcPrice(act, service), false, dur, false, Pay.CASH, 0);
        if (id < 0) {
            Ui.toast(act, "ثبت نوبت انجام نشد");
            return false;
        }
        removePending(act, o.optLong("ts"));
        sendSms(act, phone, "نوبت شما تأیید و ثبت شد.\n" + service + "\n"
                + JalaliCalendar.gregorianStringToJalali(iso) + " ساعت " + time + "\n" + AppConfig.getSalonName(act));
        Ui.toast(act, "نوبت ثبت شد و پیامک تأیید رفت");
        return true;
    }

    private static void reject(MainActivity act, org.json.JSONObject o) {
        removePending(act, o.optLong("ts"));
        sendSms(act, o.optString("phone"), "متاسفانه این ساعت ممکن نیست. برای انتخاب ساعت دیگر «"
                + AppConfig.getBookingKeyword(act) + "» را بفرستید.");
        Ui.toast(act, "درخواست رد شد و پیامک رفت");
    }

    /** فهرست درخواست‌های منتظر تأیید */
    static void showRequests(final MainActivity act) {
        final int accent = Ui.themeColor(act, R.attr.barberAccent, 0xFF1F9D84);
        final int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        final int sec = Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888);
        final android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 16), Ui.dp(act, 8), Ui.dp(act, 16), Ui.dp(act, 8));
        final androidx.appcompat.app.AlertDialog d = new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("📋 درخواست‌های پیامکی")
                .setView(Ui.scroll(box))
                .setPositiveButton("بستن", null)
                .create();
        final Runnable[] rebuild = {null};
        rebuild[0] = new Runnable() {
            @Override
            public void run() {
                box.removeAllViews();
                final org.json.JSONArray a = loadPending(act);
                if (a.length() == 0) {
                    android.widget.TextView t = new android.widget.TextView(act);
                    t.setText("درخواستی منتظر تأیید نیست.");
                    t.setTextColor(sec);
                    t.setPadding(0, Ui.dp(act, 12), 0, Ui.dp(act, 12));
                    box.addView(t);
                    return;
                }
                for (int i = 0; i < a.length(); i++) {
                    final org.json.JSONObject o = a.optJSONObject(i);
                    if (o == null) continue;
                    android.widget.LinearLayout card = new android.widget.LinearLayout(act);
                    card.setOrientation(android.widget.LinearLayout.VERTICAL);
                    card.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
                    card.setBackground(Ui.tinted(act, prim, 0x0F, 14));
                    android.widget.LinearLayout.LayoutParams cl = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    cl.bottomMargin = Ui.dp(act, 10);

                    String nm = customerNameByPhone(act, o.optString("phone"));
                    android.widget.TextView t1 = new android.widget.TextView(act);
                    t1.setText((nm.isEmpty() ? "" : nm + " • ") + o.optString("phone"));
                    t1.setTextColor(prim);
                    t1.setTextSize(15);
                    t1.setTypeface(t1.getTypeface(), android.graphics.Typeface.BOLD);
                    card.addView(t1);
                    android.widget.TextView t2 = new android.widget.TextView(act);
                    t2.setText(o.optString("service") + " • "
                            + JalaliCalendar.gregorianStringToJalali(o.optString("date"))
                            + " ساعت " + o.optString("time"));
                    t2.setTextColor(sec);
                    t2.setTextSize(13);
                    t2.setPadding(0, Ui.dp(act, 4), 0, Ui.dp(act, 8));
                    card.addView(t2);

                    android.widget.LinearLayout row = new android.widget.LinearLayout(act);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
                    android.widget.TextView ok = new android.widget.TextView(act);
                    ok.setText("✅ تأیید");
                    ok.setTextColor(0xFFFFFFFF);
                    ok.setBackground(Ui.tinted(act, accent, 0xFF, 12));
                    android.widget.TextView no = new android.widget.TextView(act);
                    no.setText("❌ رد");
                    no.setTextColor(0xFFD9534F);
                    no.setBackground(Ui.tinted(act, 0xFFD9534F, 0x26, 12));
                    android.widget.TextView[] bs = {ok, no};
                    for (android.widget.TextView b : bs) {
                        b.setTextSize(14);
                        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
                        b.setGravity(android.view.Gravity.CENTER);
                        b.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 10));
                    }
                    android.widget.LinearLayout.LayoutParams l1 = new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    android.widget.LinearLayout.LayoutParams l2 = new android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    l2.setMargins(Ui.dp(act, 8), 0, 0, 0);
                    row.addView(ok, l1);
                    row.addView(no, l2);
                    card.addView(row);
                    ok.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            if (!act.canWrite()) return;
                            if (approve(act, o)) rebuild[0].run();
                        }
                    });
                    no.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            reject(act, o);
                            rebuild[0].run();
                        }
                    });
                    box.addView(card, cl);
                }
            }
        };
        rebuild[0].run();
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface di) {
                fireChanged();
            }
        });
        d.show();
    }

    /** ساعت‌های خالی آن روز که خدمت در آن جا می‌شود (حداکثر ۹ مورد). ساعت‌های گذشته‌ی امروز حذف می‌شوند. */
    static java.util.List<String> freeSlots(android.content.Context ctx, String iso, String service) {
        java.util.List<String> out = new java.util.ArrayList<>();
        Db db = Db.get(ctx);
        int dur = svcDuration(ctx, service);
        int workStart = AppConfig.getWorkStartHour(ctx) * 60;
        int workEnd = AppConfig.getWorkEndHour(ctx) * 60;
        java.util.List<Db.Appointment> appts = db.getAppointments(iso, null);
        int[] st = new int[appts.size()];
        int[] du = new int[appts.size()];
        for (int i = 0; i < st.length; i++) {
            st[i] = Logic.toMinutes(appts.get(i).time);
            du[i] = appts.get(i).duration;
        }
        java.util.List<int[]> segs = Logic.daySegments(workStart, workEnd, AppConfig.getTimeMode(ctx), st, du);
        int now = -1;
        if (iso.equals(JalaliCalendar.todayGregorianString())) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            now = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE) + 15;
        }
        java.util.Set<String> pend = pendingTimes(ctx, iso);
        for (int[] g : segs) {
            if (g[2] != 0) continue;
            if (g[0] < now) continue;
            if (g[0] + dur > workEnd) continue;
            String t = Logic.fromMinutes(g[0]);
            if (pend.contains(t)) continue;
            if (db.findConflict(iso, t, dur, -1) != null) continue;
            if (db.closedAt(iso, g[0], dur) != null) continue; // ناهار، تعطیلی...
            out.add(t);
            if (out.size() >= MAX_SERVICES) break;
        }
        return out;
    }

    private static String slotMenu(String iso, java.util.List<String> slots) {
        StringBuilder sb = new StringBuilder("ساعت‌های خالی " + JalaliCalendar.gregorianStringToJalali(iso) + ":");
        for (int i = 0; i < slots.size(); i++) sb.append("\n").append(i + 1).append(" ساعت ").append(slots.get(i));
        return sb.toString();
    }

    // درخواست‌های منتظر تأیید (حالت «با تأیید من»؛ صفحه‌ی تأیید در مرحله‌ی ۶ ساخته می‌شود)
    private static java.util.Set<String> pendingTimes(android.content.Context ctx, String iso) {
        java.util.Set<String> r = new java.util.HashSet<>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(ctx.getSharedPreferences("booking_pending",
                    android.content.Context.MODE_PRIVATE).getString("list", "[]"));
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.getJSONObject(i);
                if (iso.equals(o.optString("date"))) r.add(o.optString("time"));
            }
        } catch (Exception ignored) {
        }
        return r;
    }

    private static void addPending(android.content.Context ctx, String phone, String service, String iso, String time) {
        try {
            android.content.SharedPreferences sp = ctx.getSharedPreferences("booking_pending", android.content.Context.MODE_PRIVATE);
            org.json.JSONArray a = new org.json.JSONArray(sp.getString("list", "[]"));
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("phone", phone);
            o.put("service", service);
            o.put("date", iso);
            o.put("time", time);
            o.put("ts", System.currentTimeMillis());
            a.put(o);
            sp.edit().putString("list", a.toString()).apply();
            notifyOwner(ctx, "📋 درخواست نوبت جدید", who(ctx, phone) + "\n" + service + " • "
                    + JalaliCalendar.gregorianStringToJalali(iso) + " ساعت " + time, true);
            fireChanged();
        } catch (Exception ignored) {
        }
    }

    /** مشتری را با شماره پیدا می‌کند؛ اگر نبود می‌سازد. */
    static long findOrCreateCustomer(android.content.Context ctx, String phone) {
        Db db = Db.get(ctx);
        for (Db.Customer c : db.getCustomers()) {
            if (phone.equals(normPhone(c.phone))) return c.id;
        }
        return db.addCustomer("مشتری " + phone, phone);
    }

    /** ثبت نهایی (یا درخواست منتظر تأیید) و پیامک نتیجه */
    private static void finishBooking(android.content.Context ctx, String phone, String service, String iso, String time) {
        String when = JalaliCalendar.gregorianStringToJalali(iso) + " ساعت " + time;
        if (!AppConfig.getBookingAuto(ctx)) {
            addPending(ctx, phone, service, iso, time);
            clearSession(ctx, phone);
            sendSms(ctx, phone, "درخواست شما برای " + service + "، " + when
                    + " ثبت شد. بعد از تأیید آرایشگر پیامک می‌گیرید.");
            return;
        }
        int dur = svcDuration(ctx, service);
        Db db = Db.get(ctx);
        if (db.findConflict(iso, time, dur, -1) != null || db.closedAt(iso, Logic.toMinutes(time), dur) != null) {
            java.util.List<String> slots = freeSlots(ctx, iso, service);
            if (slots.isEmpty()) {
                clearSession(ctx, phone);
                sendSms(ctx, phone, "متاسفانه این ساعت پر شد و این روز وقت خالی ندارد. برای شروع دوباره «"
                        + AppConfig.getBookingKeyword(ctx) + "» را بفرستید.");
                return;
            }
            try {
                org.json.JSONObject s = loadSession(ctx, phone);
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String t : slots) arr.put(t);
                s.put("slots", arr);
                saveSession(ctx, phone, s);
            } catch (Exception ignored) {
            }
            sendSms(ctx, phone, "این ساعت همین الان پر شد.\n" + slotMenu(iso, slots));
            return;
        }
        long cid = findOrCreateCustomer(ctx, phone);
        long id = cid < 0 ? -1 : db.addAppointment(cid, iso, time, service,
                svcPrice(ctx, service), false, dur, false, Pay.CASH, 0);
        clearSession(ctx, phone);
        if (id < 0) {
            sendSms(ctx, phone, "ثبت نوبت انجام نشد. لطفاً با آرایشگاه تماس بگیرید.");
            return;
        }
        notifyOwner(ctx, "✅ نوبت جدید با پیامک", who(ctx, phone) + "\n" + service + " • " + when);
        sendSms(ctx, phone, "نوبت شما ثبت شد.\n" + service + "\n" + when + "\n" + AppConfig.getSalonName(ctx));
    }

    // ---------------- لغو نوبت و محدودیت‌ها (مرحله ۷) ----------------

    /** علامت نامرئی انتهای پیامک‌های خودکار (برای شناختن پیامک یک برنامه‌ی دیگر) */
    static final char BOT_MARK = '\u2063';
    private static final long LOOP_WINDOW_MS = 10L * 60L * 1000L;
    private static final long LOOP_BLOCK_MS = 60L * 60L * 1000L;
    private static final int LOOP_MAX_IN = 12;
    private static final int GLOBAL_MAX_IN = 40;
    private static final int GLOBAL_MAX_OUT = 80;
    private static final int MAX_ACTIVE_PER_PHONE = 3;

    /** نوبت‌دهی خودکار را خاموش می‌کند و به آرایشگر اعلان می‌دهد؛ روشن‌کردنش فقط از تنظیمات و با خود آرایشگر است */
    private static void turnOffForSafety(android.content.Context ctx, String why) {
        if (!AppConfig.getBookingOn(ctx)) return;
        AppConfig.setBookingOn(ctx, false);
        limits(ctx).edit().putInt("gwc", 0).putInt("owc", 0).apply();
        notifyOwner(ctx, "⛔ نوبت‌دهی پیامکی خاموش شد",
                why + ". برای جلوگیری از پیامک‌های ناخواسته خاموش شد؛ بعد از بررسی، از منوی «نوبت‌گیری پیامکی» دوباره روشنش کنید.");
        fireChanged();
    }
    private static final int MAX_IN_PER_DAY = 40;
    private static final int MAX_OUT_PER_DAY = 300;

    private static android.content.SharedPreferences limits(android.content.Context ctx) {
        return ctx.getSharedPreferences("booking_limits", android.content.Context.MODE_PRIVATE);
    }

    /** محدودیت پیامک ورودی: فاصله‌ی حداقل ۲ ثانیه و حداکثر ۴۰ پیامک در روز از هر شماره */
    private static boolean allowIncoming(android.content.Context ctx, String phone) {
        android.content.SharedPreferences sp = limits(ctx);
        long now = System.currentTimeMillis();
        // محافظ حلقه: اگر یک شماره در ۱۰ دقیقه بیش از ۱۲ پیامک فرستاد، ۱ ساعت نادیده گرفته می‌شود
        if (now < sp.getLong("b_" + phone, 0)) return false;
        long ws = sp.getLong("ws_" + phone, 0);
        int wc = sp.getInt("wc_" + phone, 0);
        if (now - ws > LOOP_WINDOW_MS) {
            ws = now;
            wc = 0;
        }
        wc++;
        if (wc > LOOP_MAX_IN) {
            sp.edit().putLong("b_" + phone, now + LOOP_BLOCK_MS).putInt("wc_" + phone, 0).apply();
            notifyOwner(ctx, "⚠️ پیامک‌های پشت‌سرهم", "از شماره‌ی " + who(ctx, phone)
                    + " پیامک‌ها خیلی زیاد آمد؛ یک ساعت جواب داده نمی‌شود.");
            return false;
        }
        // محافظ کلی: انبوه پیامک از شماره‌های مختلف → نوبت‌دهی خودکار خاموش می‌شود (فقط آرایشگر روشنش می‌کند)
        long gs = sp.getLong("gws", 0);
        int gc = sp.getInt("gwc", 0);
        if (now - gs > LOOP_WINDOW_MS) {
            gs = now;
            gc = 0;
        }
        gc++;
        sp.edit().putLong("gws", gs).putInt("gwc", gc).apply();
        if (gc > GLOBAL_MAX_IN) {
            turnOffForSafety(ctx, "تعداد پیامک‌های دریافتی در ۱۰ دقیقه خیلی زیاد بود");
            return false;
        }
        sp.edit().putLong("ws_" + phone, ws).putInt("wc_" + phone, wc).apply();
        String today = JalaliCalendar.todayGregorianString();
        String v = sp.getString("d_" + phone, "");
        int cnt = 0;
        if (v.startsWith(today + "|")) {
            try {
                cnt = Integer.parseInt(v.substring(today.length() + 1));
            } catch (Exception ignored) {
            }
        }
        if (cnt >= MAX_IN_PER_DAY) return false;
        if (now - sp.getLong("t_" + phone, 0) < 2000) return false;
        sp.edit().putString("d_" + phone, today + "|" + (cnt + 1)).putLong("t_" + phone, now).apply();
        return true;
    }

    /** سقف پیامک ارسالی در روز (برای حفظ سیم‌کارت از محدودیت اپراتور) */
    private static boolean allowOutgoing(android.content.Context ctx) {
        android.content.SharedPreferences sp = limits(ctx);
        String today = JalaliCalendar.todayGregorianString();
        String v = sp.getString("out", "");
        int cnt = 0;
        if (v.startsWith(today + "|")) {
            try {
                cnt = Integer.parseInt(v.substring(today.length() + 1));
            } catch (Exception ignored) {
            }
        }
        if (cnt >= MAX_OUT_PER_DAY) return false;
        long nowMs = System.currentTimeMillis();
        long os = sp.getLong("ows", 0);
        int oc = sp.getInt("owc", 0);
        if (nowMs - os > LOOP_WINDOW_MS) {
            os = nowMs;
            oc = 0;
        }
        oc++;
        if (oc > GLOBAL_MAX_OUT) {
            turnOffForSafety(ctx, "تعداد پیامک‌های ارسالی در ۱۰ دقیقه خیلی زیاد بود");
            return false;
        }
        sp.edit().putString("out", today + "|" + (cnt + 1)).putLong("ows", os).putInt("owc", oc).apply();
        return true;
    }

    /** نوبت‌های آینده‌ی انجام‌نشده‌ی این شماره */
    private static java.util.List<Db.Appointment> upcomingFor(android.content.Context ctx, String phone) {
        java.util.List<Db.Appointment> out = new java.util.ArrayList<>();
        for (Db.Appointment a : Db.get(ctx).getAppointmentsFrom(JalaliCalendar.todayGregorianString())) {
            if (!a.done && phone.equals(normPhone(a.phone))) out.add(a);
        }
        return out;
    }

    private static int pendingFor(android.content.Context ctx, String phone) {
        int n = 0;
        org.json.JSONArray a = loadPending(ctx);
        for (int i = 0; i < a.length(); i++) {
            if (phone.equals(a.optJSONObject(i).optString("phone"))) n++;
        }
        return n;
    }

    private static void startCancel(android.content.Context ctx, String phone) {
        try {
            org.json.JSONArray items = new org.json.JSONArray();
            StringBuilder sb = new StringBuilder("کدام نوبت لغو شود؟");
            int i = 1;
            for (Db.Appointment a : upcomingFor(ctx, phone)) {
                if (i > 5) break;
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("t", "a");
                o.put("id", a.id);
                items.put(o);
                sb.append("\n").append(i++).append(" ").append(a.service).append(" ")
                        .append(JalaliCalendar.gregorianStringToJalali(a.date)).append(" ساعت ").append(a.time);
            }
            org.json.JSONArray pend = loadPending(ctx);
            for (int k = 0; k < pend.length() && i <= 5; k++) {
                org.json.JSONObject p = pend.getJSONObject(k);
                if (!phone.equals(p.optString("phone"))) continue;
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("t", "p");
                o.put("ts", p.optLong("ts"));
                items.put(o);
                sb.append("\n").append(i++).append(" ").append(p.optString("service")).append(" ")
                        .append(JalaliCalendar.gregorianStringToJalali(p.optString("date")))
                        .append(" ساعت ").append(p.optString("time")).append(" (منتظر تأیید)");
            }
            if (items.length() == 0) {
                sendSms(ctx, phone, "نوبتی برای این شماره پیدا نشد.");
                return;
            }
            sb.append("\nانصراف: لغو");
            org.json.JSONObject s = new org.json.JSONObject();
            s.put("step", "cancel");
            s.put("items", items);
            saveSession(ctx, phone, s);
            sendSms(ctx, phone, sb.toString());
        } catch (Exception ignored) {
        }
    }

    /** پردازش یک پیامک ورودی (خدمت، روز، ساعت، ثبت نوبت) */
    static void handle(android.content.Context ctx, String from, String body, int subId) {
        String phone = normPhone(from);
        if (phone.isEmpty()) return;
        if (LicenseManager.state(ctx).readOnly()) return;
        // پیامکی که خودِ یک برنامه‌ی نوبت‌گیری فرستاده (علامت نامرئی دارد) هرگز جواب نمی‌گیرد؛ جلوی پینگ‌پنگ دو برنامه
        if (body != null && body.indexOf(BOT_MARK) >= 0) return;
        String text = normalize(body);
        boolean start = isStart(ctx, body);
        boolean cancelNow = text.equals("لغو نوبت");
        org.json.JSONObject s = loadSession(ctx, phone);
        if (!start && !cancelNow && s == null) return; // پیامکِ بی‌ربط (شخصی، تبلیغاتی...) جواب نمی‌گیرد
        if (!allowIncoming(ctx, phone)) return;
        limits(ctx).edit().putInt("s_" + phone, subId).apply(); // پاسخ از همان سیم‌کارتی که پیامک را گرفته می‌رود
        if (start) {
            startFlow(ctx, phone);
            return;
        }
        if (cancelNow) {
            startCancel(ctx, phone);
            return;
        }
        String key = AppConfig.getBookingKeyword(ctx);
        if (System.currentTimeMillis() - s.optLong("ts") > SESSION_MS) {
            clearSession(ctx, phone);
            sendSms(ctx, phone, "زمان گفت‌وگو تمام شد. برای شروع دوباره «" + key + "» را بفرستید.");
            return;
        }
        if (text.equals("لغو")) {
            clearSession(ctx, phone);
            sendSms(ctx, phone, "انجام شد. هر وقت خواستید «" + key + "» را بفرستید.");
            return;
        }
        int n = text.matches("\\d{1,2}") ? Integer.parseInt(text) : -1;
        String step = s.optString("step");
        try {
            if ("service".equals(step)) {
                java.util.List<String> l = bookableServices(ctx);
                java.util.List<Integer> nums = parseNums(text);
                java.util.List<String> picked = new java.util.ArrayList<>();
                if (nums != null) {
                    for (int k : nums) {
                        if (k >= 1 && k <= l.size() && !picked.contains(l.get(k - 1))) picked.add(l.get(k - 1));
                        else if (k < 1 || k > l.size()) {
                            picked.clear();
                            break;
                        }
                    }
                }
                if (picked.isEmpty()) {
                    sendSms(ctx, phone, "شماره درست نیست. عددی از 1 تا " + l.size()
                            + " بفرستید (چند خدمت: 1+3) یا «لغو».");
                    return;
                }
                if (picked.size() > 1) {
                    for (String nm : picked) {
                        if (!AppConfig.getPackage(ctx, nm).isEmpty()) {
                            sendSms(ctx, phone, "«" + nm + "» را باید تنها انتخاب کنید.");
                            return;
                        }
                    }
                }
                s.put("service", Logic.joinServices(picked));
                java.util.List<String> days = openDays(ctx);
                if (days.isEmpty()) {
                    clearSession(ctx, phone);
                    sendSms(ctx, phone, "فعلاً نوبت‌گیری با پیامک ممکن نیست. لطفاً با آرایشگاه تماس بگیرید.");
                    return;
                }
                org.json.JSONArray dArr = new org.json.JSONArray();
                for (String dd : days) dArr.put(dd);
                s.put("days", dArr);
                s.put("step", "day");
                saveSession(ctx, phone, s);
                sendSms(ctx, phone, dayMenu(days));
            } else if ("day".equals(step)) {
                org.json.JSONArray dArr = s.optJSONArray("days");
                int dCnt = dArr == null ? 0 : dArr.length();
                if (n < 1 || n > dCnt) {
                    sendSms(ctx, phone, "شماره درست نیست. عددی از 1 تا " + dCnt + " بفرستید یا «لغو».");
                    return;
                }
                String iso = dArr.getString(n - 1);
                java.util.List<String> slots = freeSlots(ctx, iso, s.optString("service"));
                if (slots.isEmpty()) {
                    java.util.List<String> days = new java.util.ArrayList<>();
                    for (int k = 0; k < dCnt; k++) days.add(dArr.getString(k));
                    sendSms(ctx, phone, "این روز وقت خالی ندارد.\n" + dayMenu(days));
                    return;
                }
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String t : slots) arr.put(t);
                s.put("date", iso);
                s.put("slots", arr);
                s.put("step", "slot");
                saveSession(ctx, phone, s);
                sendSms(ctx, phone, slotMenu(iso, slots));
            } else if ("cancel".equals(step)) {
                org.json.JSONArray items = s.optJSONArray("items");
                int cnt = items == null ? 0 : items.length();
                if (n < 1 || n > cnt) {
                    sendSms(ctx, phone, "شماره درست نیست. عددی از 1 تا " + cnt + " بفرستید یا «لغو».");
                    return;
                }
                org.json.JSONObject it = items.getJSONObject(n - 1);
                if ("a".equals(it.optString("t"))) {
                    Db.Appointment ap = Db.get(ctx).getAppointmentById(it.optLong("id"));
                    Db.get(ctx).deleteAppointment(it.optLong("id"));
                    if (ap != null) {
                        notifyOwner(ctx, "❌ نوبت با پیامک لغو شد", who(ctx, phone) + "\n" + ap.service + " • "
                                + JalaliCalendar.gregorianStringToJalali(ap.date) + " ساعت " + ap.time);
                    }
                } else {
                    removePending(ctx, it.optLong("ts"));
                }
                clearSession(ctx, phone);
                sendSms(ctx, phone, "نوبت شما لغو شد. برای نوبت جدید «" + AppConfig.getBookingKeyword(ctx) + "» را بفرستید.");
            } else if ("slot".equals(step)) {
                org.json.JSONArray arr = s.optJSONArray("slots");
                int cnt = arr == null ? 0 : arr.length();
                if (n < 1 || n > cnt) {
                    sendSms(ctx, phone, "شماره درست نیست. عددی از 1 تا " + cnt + " بفرستید یا «لغو».");
                    return;
                }
                finishBooking(ctx, phone, s.optString("service"), s.optString("date"), arr.getString(n - 1));
            }
        } catch (Exception ignored) {
        }
    }

    /** ارسال پیامک (چندبخشی برای متن بلند). false اگر ارسال نشد. */
    static boolean sendSms(android.content.Context ctx, String to, String text) {
        if (!allowOutgoing(ctx)) return false;
        try {
            android.telephony.SmsManager sm = null;
            int sub = limits(ctx).getInt("s_" + to, -1);
            if (sub >= 0) {
                try {
                    sm = android.telephony.SmsManager.getSmsManagerForSubscriptionId(sub);
                } catch (Exception ignored) {
                    sm = null;
                }
            }
            if (sm == null) {
                sm = android.os.Build.VERSION.SDK_INT >= 31
                        ? ctx.getSystemService(android.telephony.SmsManager.class)
                        : android.telephony.SmsManager.getDefault();
            }
            java.util.ArrayList<String> parts = sm.divideMessage(text + BOT_MARK);
            sm.sendMultipartTextMessage(to, null, parts, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private BookingUi() {
    }

    static boolean hasPermissions(MainActivity act) {
        return act.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
                && act.checkSelfPermission(android.Manifest.permission.SEND_SMS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    static void onPermissionResult(MainActivity act, int requestCode, int[] grants) {
        if (requestCode != REQ_BOOKING) return;
        boolean ok = grants.length >= 2;
        for (int g : grants) {
            if (g != android.content.pm.PackageManager.PERMISSION_GRANTED) ok = false;
        }
        if (ok) {
            Ui.toast(act, "نوبت‌گیری پیامکی فعال شد");
        } else {
            AppConfig.setBookingOn(act, false);
            Ui.toastLong(act, "بدون اجازه‌ی دریافت و ارسال پیامک، نوبت‌گیری فعال نمی‌شود");
        }
    }

    static void show(final MainActivity act) {
        if (!AppConfig.bookingEdition()) return;
        final int accent = Ui.themeColor(act, R.attr.barberAccent, 0xFF1F9D84);
        final int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        final int sec = Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888);

        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 12), Ui.dp(act, 20), Ui.dp(act, 8));

        android.widget.TextView intro = new android.widget.TextView(act);
        intro.setText("مشتری برای شماره‌ی همین گوشی پیامک می‌دهد و برنامه با چند پیامک ساده نوبت را می‌گیرد. "
                + "نوبت‌ها برای آرایشگر اصلی (دفتر نوبت خودتان) ثبت می‌شود.");
        intro.setTextSize(13);
        intro.setTextColor(sec);
        box.addView(intro);

        final boolean licensed = LicenseManager.smsLicensed(act);
        android.widget.TextView licNote = new android.widget.TextView(act);
        licNote.setTextSize(13);
        licNote.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
        if (licensed) {
            licNote.setText("✅ لایسنس نوبت‌دهی پیامکی تا " + LicenseManager.dateText(LicenseManager.smsEnd(act)) + " فعال است.");
            licNote.setTextColor(0xFF1B7F3B);
            licNote.setBackground(Ui.tinted(act, 0xFF1B7F3B, 0x1F, 12));
        } else {
            licNote.setText("🔒 لایسنس نوبت‌دهی پیامکی ندارید یا تمام شده است. برای فعال‌سازی، کد لایسنس «نوبت‌دهی پیامکی» را از رایا سیستم بگیرید "
                    + "و در بخش «لایسنس» وارد کنید. بقیه‌ی برنامه با لایسنس عادی کار می‌کند.");
            licNote.setTextColor(0xFFB3261E);
            licNote.setBackground(Ui.tinted(act, 0xFFB3261E, 0x1F, 12));
        }
        android.widget.LinearLayout.LayoutParams lnp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lnp.setMargins(0, Ui.dp(act, 10), 0, 0);
        box.addView(licNote, lnp);

        final androidx.appcompat.widget.SwitchCompat sw = new androidx.appcompat.widget.SwitchCompat(act);
        sw.setText("نوبت‌گیری با پیامک روشن باشد");
        sw.setChecked(licensed && AppConfig.getBookingOn(act));
        sw.setEnabled(licensed);
        sw.setPadding(0, Ui.dp(act, 14), 0, Ui.dp(act, 6));
        box.addView(sw);

        android.widget.TextView l1 = new android.widget.TextView(act);
        l1.setText("کلمه‌ی شروع (مشتری باید همین را بفرستد)");
        l1.setTextSize(12);
        l1.setTextColor(sec);
        l1.setPadding(Ui.dp(act, 4), Ui.dp(act, 12), 0, Ui.dp(act, 2));
        box.addView(l1);
        final android.widget.EditText etKey = new android.widget.EditText(act);
        etKey.setText(AppConfig.getBookingKeyword(act));
        etKey.setSingleLine(true);
        box.addView(etKey);

        android.widget.TextView l2 = new android.widget.TextView(act);
        l2.setText("ثبت نوبت");
        l2.setTextSize(12);
        l2.setTextColor(sec);
        l2.setPadding(Ui.dp(act, 4), Ui.dp(act, 12), 0, Ui.dp(act, 4));
        box.addView(l2);

        final boolean[] auto = {AppConfig.getBookingAuto(act)};
        final android.widget.TextView[] chips = new android.widget.TextView[2];
        String[] names = {"✅ خودکار", "🖐 با تأیید من"};
        android.widget.LinearLayout row = new android.widget.LinearLayout(act);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 2; i++) {
                    boolean on = (i == 0) == auto[0];
                    chips[i].setTextColor(on ? 0xFFFFFFFF : prim);
                    chips[i].setBackground(on ? Ui.tinted(act, accent, 0xFF, 12) : Ui.tinted(act, prim, 0x14, 12));
                }
            }
        };
        for (int i = 0; i < 2; i++) {
            final boolean isAuto = i == 0;
            android.widget.TextView c = new android.widget.TextView(act);
            c.setText(names[i]);
            c.setTextSize(14);
            c.setTypeface(c.getTypeface(), android.graphics.Typeface.BOLD);
            c.setGravity(android.view.Gravity.CENTER);
            c.setPadding(0, Ui.dp(act, 11), 0, Ui.dp(act, 11));
            c.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    auto[0] = isAuto;
                    paint.run();
                }
            });
            chips[i] = c;
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMargins(Ui.dp(act, 8), 0, 0, 0);
            row.addView(c, lp);
        }
        paint.run();
        box.addView(row);

        final android.widget.TextView bat = new android.widget.TextView(act);
        bat.setTextSize(13);
        bat.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
        boolean okBat = batteryUnrestricted(act);
        bat.setText(okBat ? "✅ برنامه از محدودیت باتری آزاد است"
                : "⚠ ممکن است گوشی برنامه را در پس‌زمینه ببندد و پیامک‌ها بی‌جواب بمانند. اینجا را لمس کنید و «اجازه بده» را بزنید.");
        bat.setTextColor(okBat ? accent : 0xFFC77D00);
        bat.setBackground(Ui.tinted(act, okBat ? accent : 0xFFE08A00, 0x1F, 12));
        if (!okBat) {
            bat.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    openBatterySettings(act);
                }
            });
        }
        android.widget.LinearLayout.LayoutParams bl = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bl.topMargin = Ui.dp(act, 16);
        box.addView(bat, bl);

        android.widget.TextView note = new android.widget.TextView(act);
        note.setText("• خودکار: نوبت بعد از انتخاب مشتری همان لحظه ثبت می‌شود.\n"
                + "• با تأیید من: نوبت تا تأیید شما ثبت نمی‌شود.\n\n"
                + "گوشی باید روشن بماند و سیم‌کارت اعتبار داشته باشد. "
                + "اگر برنامه را «بستن اجباری» کنید، یک بار دوباره بازش کنید. "
                + "در گوشی‌های شیائومی و سامسونگ در تنظیمات برنامه «شروع خودکار» و «بدون محدودیت باتری» را هم بزنید. "
                + "در گوشی دو سیم‌کارته، پاسخ از همان سیم‌کارتی می‌رود که پیامک را گرفته. "
                + "هر پیامکِ پاسخ از سیم‌کارت خودتان حساب می‌شود.");
        note.setTextSize(12);
        note.setTextColor(sec);
        note.setPadding(0, Ui.dp(act, 12), 0, 0);
        box.addView(note);

        final androidx.appcompat.app.AlertDialog d = new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("📩 نوبت‌گیری پیامکی")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null)
                .setNeutralButton("📋 درخواست‌ها (" + pendingCount(act) + ")", null)
                .create();
        d.show();
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                d.dismiss();
                showRequests(act);
            }
        });
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                String key = etKey.getText().toString().trim();
                if (key.isEmpty() || key.length() > 15) {
                    etKey.setError("کلمه‌ی شروع باید ۱ تا ۱۵ حرف باشد");
                    return;
                }
                if (!act.canWrite()) return;
                boolean on = sw.isChecked();
                AppConfig.setBooking(act, on, key, auto[0]);
                d.dismiss();
                if (on && !hasPermissions(act)) {
                    act.requestPermissions(new String[]{android.Manifest.permission.RECEIVE_SMS,
                            android.Manifest.permission.SEND_SMS}, REQ_BOOKING);
                } else {
                    Ui.toast(act, on ? "نوبت‌گیری پیامکی فعال شد" : "ذخیره شد");
                }
            }
        });
    }
}


/** صفحه‌ی «زمان‌های بسته»: ساعت‌هایی (مثل ناهار) یا روزهایی (مثل جمعه‌ها، مسافرت) که نوبت داده نمی‌شود. */
final class ClosedUi {

    private ClosedUi() {
    }

    static String repeatText(Db.ClosedTime t) {
        if ("weekly".equals(t.repeat)) {
            if (t.weekdays == 127) return "هر روز";
            StringBuilder sb = new StringBuilder("هر ");
            boolean first = true;
            for (int i = 0; i < 7; i++) {
                if ((t.weekdays & (1 << i)) != 0) {
                    if (!first) sb.append("، ");
                    sb.append(JalaliCalendar.DAY_NAMES[i]);
                    first = false;
                }
            }
            return first ? "هر هفته" : sb.toString();
        }
        if ("date".equals(t.repeat)) return JalaliCalendar.gregorianStringToJalali(t.dateFrom);
        return "از " + JalaliCalendar.gregorianStringToJalali(t.dateFrom)
                + " تا " + JalaliCalendar.gregorianStringToJalali(t.dateTo);
    }

    static String timeText(Db.ClosedTime t) {
        return t.allDay ? "کل روز" : Logic.fromMinutes(t.startMin) + " تا " + Logic.fromMinutes(t.endMin);
    }

    static void show(final MainActivity act) {
        final int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        final int sec = Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888);
        final android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 16), Ui.dp(act, 8), Ui.dp(act, 16), Ui.dp(act, 8));
        final androidx.appcompat.app.AlertDialog d = new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle("🚫 زمان‌های بسته")
                .setView(Ui.scroll(box))
                .setPositiveButton("بستن", null)
                .setNeutralButton("+ زمان بسته جدید", null)
                .create();
        final Runnable[] rebuild = {null};
        rebuild[0] = new Runnable() {
            @Override
            public void run() {
                box.removeAllViews();
                android.widget.TextView info = new android.widget.TextView(act);
                info.setText("ساعت‌ها یا روزهایی که نمی‌خواهید نوبت داده شود (ناهار، جمعه‌ها، مسافرت...). "
                        + "این زمان‌ها در نوبت‌گیری پیامکی به مشتری پیشنهاد نمی‌شود.");
                info.setTextSize(12);
                info.setTextColor(sec);
                info.setPadding(0, 0, 0, Ui.dp(act, 10));
                box.addView(info);
                java.util.List<Db.ClosedTime> list = Db.get(act).getClosedTimes();
                if (list.isEmpty()) {
                    android.widget.TextView e = new android.widget.TextView(act);
                    e.setText("هنوز زمان بسته‌ای ثبت نشده. با «+ زمان بسته جدید» شروع کنید.");
                    e.setTextColor(sec);
                    box.addView(e);
                    return;
                }
                for (final Db.ClosedTime t : list) {
                    android.widget.LinearLayout card = new android.widget.LinearLayout(act);
                    card.setOrientation(android.widget.LinearLayout.VERTICAL);
                    card.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
                    card.setBackground(Ui.tinted(act, prim, 0x0F, 14));
                    android.widget.LinearLayout.LayoutParams cl = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    cl.bottomMargin = Ui.dp(act, 8);
                    android.widget.TextView t1 = new android.widget.TextView(act);
                    t1.setText((t.allDay ? "🚫 " : "⏰ ") + repeatText(t) + " • " + timeText(t));
                    t1.setTextColor(prim);
                    t1.setTextSize(15);
                    t1.setTypeface(t1.getTypeface(), android.graphics.Typeface.BOLD);
                    card.addView(t1);
                    if (!t.note.isEmpty()) {
                        android.widget.TextView t2 = new android.widget.TextView(act);
                        t2.setText(t.note);
                        t2.setTextColor(sec);
                        t2.setTextSize(13);
                        card.addView(t2);
                    }
                    card.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            edit(act, t, rebuild[0]);
                        }
                    });
                    box.addView(card, cl);
                }
            }
        };
        rebuild[0].run();
        d.show();
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                edit(act, null, rebuild[0]);
            }
        });
    }

    private static android.widget.TextView chip(final MainActivity act, String text) {
        android.widget.TextView c = new android.widget.TextView(act);
        c.setText(text);
        c.setTextSize(13);
        c.setGravity(android.view.Gravity.CENTER);
        c.setPadding(Ui.dp(act, 10), Ui.dp(act, 9), Ui.dp(act, 10), Ui.dp(act, 9));
        return c;
    }

    private static android.widget.TextView label(MainActivity act, String text) {
        android.widget.TextView t = new android.widget.TextView(act);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(Ui.themeColor(act, android.R.attr.textColorSecondary, 0xFF888888));
        t.setPadding(Ui.dp(act, 4), Ui.dp(act, 12), 0, Ui.dp(act, 4));
        return t;
    }

    private static android.widget.LinearLayout row(MainActivity act) {
        android.widget.LinearLayout r = new android.widget.LinearLayout(act);
        r.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        r.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        return r;
    }

    private static android.widget.LinearLayout.LayoutParams weight(MainActivity act, boolean gap) {
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (gap) lp.setMargins(Ui.dp(act, 6), 0, 0, 0);
        return lp;
    }

    static void edit(final MainActivity act, final Db.ClosedTime existing, final Runnable done) {
        final int accent = Ui.themeColor(act, R.attr.barberAccent, 0xFF1F9D84);
        final int prim = Ui.themeColor(act, android.R.attr.textColorPrimary, 0xFF222222);
        final Db.ClosedTime t = new Db.ClosedTime();
        if (existing != null) {
            t.id = existing.id;
            t.allDay = existing.allDay;
            t.repeat = existing.repeat;
            t.weekdays = existing.weekdays;
            t.dateFrom = existing.dateFrom;
            t.dateTo = existing.dateTo;
            t.startMin = existing.startMin;
            t.endMin = existing.endMin;
            t.note = existing.note;
        } else {
            t.startMin = 13 * 60;
            t.endMin = 14 * 60;
            t.weekdays = 1 << 6; // جمعه
        }
        int[] td = JalaliCalendar.today();
        final String todayIso = JalaliCalendar.jalaliToGregorianString(td[0], td[1], td[2]);
        if (t.dateFrom.isEmpty()) t.dateFrom = todayIso;
        if (t.dateTo.isEmpty()) t.dateTo = t.dateFrom;

        final android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        box.setPadding(Ui.dp(act, 20), Ui.dp(act, 8), Ui.dp(act, 20), Ui.dp(act, 8));

        // نوع
        box.addView(label(act, "چه چیزی بسته باشد؟"));
        android.widget.LinearLayout typeRow = row(act);
        final android.widget.TextView cTime = chip(act, "⏰ ساعت مشخص");
        final android.widget.TextView cDay = chip(act, "🚫 کل روز");
        typeRow.addView(cTime, weight(act, false));
        typeRow.addView(cDay, weight(act, true));
        box.addView(typeRow);

        // تکرار
        box.addView(label(act, "چه موقع؟"));
        android.widget.LinearLayout repRow = row(act);
        final android.widget.TextView cWeekly = chip(act, "هر هفته");
        final android.widget.TextView cDate = chip(act, "یک روز");
        final android.widget.TextView cRange = chip(act, "چند روز");
        repRow.addView(cWeekly, weight(act, false));
        repRow.addView(cDate, weight(act, true));
        repRow.addView(cRange, weight(act, true));
        box.addView(repRow);

        // روزهای هفته
        final android.widget.LinearLayout wdBox = new android.widget.LinearLayout(act);
        wdBox.setOrientation(android.widget.LinearLayout.VERTICAL);
        final android.widget.TextView[] wd = new android.widget.TextView[7];
        android.widget.LinearLayout wr1 = row(act);
        android.widget.LinearLayout wr2 = row(act);
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            wd[i] = chip(act, JalaliCalendar.DAY_NAMES[i]);
            android.widget.LinearLayout.LayoutParams lp = weight(act, true);
            lp.topMargin = Ui.dp(act, 6);
            (i < 4 ? wr1 : wr2).addView(wd[i], lp);
        }
        wr2.addView(new android.view.View(act), weight(act, true)); // برای هم‌اندازه‌ماندن ردیف دوم
        wdBox.addView(wr1);
        wdBox.addView(wr2);
        box.addView(wdBox);

        // تاریخ‌ها
        final android.widget.LinearLayout dateBox = row(act);
        final android.widget.TextView bFrom = chip(act, "");
        final android.widget.TextView bTo = chip(act, "");
        dateBox.addView(bFrom, weight(act, false));
        dateBox.addView(bTo, weight(act, true));
        android.widget.LinearLayout.LayoutParams dl = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        dl.topMargin = Ui.dp(act, 8);
        box.addView(dateBox, dl);

        // ساعت‌ها
        final android.widget.LinearLayout timeLbl = new android.widget.LinearLayout(act);
        timeLbl.setOrientation(android.widget.LinearLayout.VERTICAL);
        timeLbl.addView(label(act, "از ساعت … تا ساعت …"));
        final android.widget.LinearLayout timeRow = row(act);
        final android.widget.TextView bStart = chip(act, "");
        final android.widget.TextView bEnd = chip(act, "");
        timeRow.addView(bStart, weight(act, false));
        timeRow.addView(bEnd, weight(act, true));
        timeLbl.addView(timeRow);
        box.addView(timeLbl);

        // توضیح
        box.addView(label(act, "توضیح (اختیاری، مثلاً ناهار)"));
        final android.widget.EditText etNote = new android.widget.EditText(act);
        etNote.setText(t.note);
        etNote.setSingleLine(true);
        box.addView(etNote);

        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                android.widget.TextView[] all = {cTime, cDay, cWeekly, cDate, cRange, bFrom, bTo, bStart, bEnd};
                for (android.widget.TextView c : all) {
                    c.setTextColor(prim);
                    c.setBackground(Ui.tinted(act, prim, 0x14, 12));
                }
                android.widget.TextView[] on = {t.allDay ? cDay : cTime,
                        "weekly".equals(t.repeat) ? cWeekly : ("date".equals(t.repeat) ? cDate : cRange)};
                for (android.widget.TextView c : on) {
                    c.setTextColor(0xFFFFFFFF);
                    c.setBackground(Ui.tinted(act, accent, 0xFF, 12));
                }
                for (int i = 0; i < 7; i++) {
                    boolean w = (t.weekdays & (1 << i)) != 0;
                    wd[i].setTextColor(w ? 0xFFFFFFFF : prim);
                    wd[i].setBackground(w ? Ui.tinted(act, accent, 0xFF, 12) : Ui.tinted(act, prim, 0x14, 12));
                }
                boolean weekly = "weekly".equals(t.repeat);
                boolean range = "range".equals(t.repeat);
                wdBox.setVisibility(weekly ? android.view.View.VISIBLE : android.view.View.GONE);
                dateBox.setVisibility(weekly ? android.view.View.GONE : android.view.View.VISIBLE);
                bTo.setVisibility(range ? android.view.View.VISIBLE : android.view.View.GONE);
                bFrom.setText((range ? "از " : "تاریخ: ") + JalaliCalendar.gregorianStringToJalali(t.dateFrom));
                bTo.setText("تا " + JalaliCalendar.gregorianStringToJalali(t.dateTo));
                timeLbl.setVisibility(t.allDay ? android.view.View.GONE : android.view.View.VISIBLE);
                bStart.setText("از " + Logic.fromMinutes(t.startMin));
                bEnd.setText("تا " + Logic.fromMinutes(t.endMin));
            }
        };
        paint.run();

        cTime.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                t.allDay = false;
                paint.run();
            }
        });
        cDay.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                t.allDay = true;
                paint.run();
            }
        });
        cWeekly.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                t.repeat = "weekly";
                paint.run();
            }
        });
        cDate.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                t.repeat = "date";
                paint.run();
            }
        });
        cRange.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                t.repeat = "range";
                if (t.dateTo.compareTo(t.dateFrom) < 0) t.dateTo = t.dateFrom;
                paint.run();
            }
        });
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            wd[i].setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    t.weekdays ^= (1 << idx);
                    paint.run();
                }
            });
        }
        bFrom.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                JalaliPicker.show(act, JalaliCalendar.jalaliOfIso(t.dateFrom), new JalaliPicker.OnPicked() {
                    @Override
                    public void onPicked(int y, int m, int dd) {
                        t.dateFrom = JalaliCalendar.jalaliToGregorianString(y, m, dd);
                        if (!"range".equals(t.repeat) || t.dateTo.compareTo(t.dateFrom) < 0) t.dateTo = t.dateFrom;
                        paint.run();
                    }
                });
            }
        });
        bTo.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                JalaliPicker.show(act, JalaliCalendar.jalaliOfIso(t.dateTo), new JalaliPicker.OnPicked() {
                    @Override
                    public void onPicked(int y, int m, int dd) {
                        t.dateTo = JalaliCalendar.jalaliToGregorianString(y, m, dd);
                        paint.run();
                    }
                });
            }
        });
        bStart.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                new android.app.TimePickerDialog(act, new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker p, int h, int m) {
                        t.startMin = h * 60 + m;
                        paint.run();
                    }
                }, t.startMin / 60, t.startMin % 60, true).show();
            }
        });
        bEnd.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                new android.app.TimePickerDialog(act, new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker p, int h, int m) {
                        t.endMin = h * 60 + m;
                        paint.run();
                    }
                }, t.endMin / 60, t.endMin % 60, true).show();
            }
        });

        com.google.android.material.dialog.MaterialAlertDialogBuilder b = new com.google.android.material.dialog.MaterialAlertDialogBuilder(act)
                .setTitle(existing == null ? "زمان بسته‌ی جدید" : "ویرایش زمان بسته")
                .setView(Ui.scroll(box))
                .setPositiveButton("ذخیره", null)
                .setNegativeButton("انصراف", null);
        if (existing != null) b.setNeutralButton("حذف", null);
        final androidx.appcompat.app.AlertDialog d = b.create();
        d.show();
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if ("weekly".equals(t.repeat) && t.weekdays == 0) {
                    Ui.toast(act, "حداقل یک روز هفته را انتخاب کنید");
                    return;
                }
                if (!t.allDay && t.endMin <= t.startMin) {
                    Ui.toast(act, "ساعت پایان باید بعد از ساعت شروع باشد");
                    return;
                }
                if ("range".equals(t.repeat) && t.dateTo.compareTo(t.dateFrom) < 0) {
                    Ui.toast(act, "تاریخ پایان باید بعد از شروع باشد");
                    return;
                }
                if (!act.canWrite()) return;
                t.note = etNote.getText().toString().trim();
                if (Db.get(act).saveClosedTime(t) < 0) {
                    Ui.toast(act, "ذخیره نشد");
                    return;
                }
                d.dismiss();
                done.run();
            }
        });
        if (existing != null) {
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    if (!act.canWrite()) return;
                    Db.get(act).deleteClosedTime(existing.id);
                    d.dismiss();
                    done.run();
                }
            });
        }
    }
}
