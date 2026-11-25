package com.barber.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * ساخت و بررسی کد لایسنس ۱۲ کاراکتری (مثل K7M2-9QX4-TH5R) که به یک دستگاه گره خورده است.
 * بدون وابستگی به اندروید؛ همین کلاس عیناً در برنامه‌ی مدیریتی هم استفاده می‌شود.
 *
 * ساختار ۶۰ بیت: [نوع ۳ بیت][روز صدور ۱۴ بیت][مقدار اضافه ۱۱ بیت][امضای ۳۲ بیتی HMAC]
 */
public final class LicenseCodec {

    /** الفبای ۳۲تایی بدون حروف شبیه هم (بدون I و L و O و U) */
    public static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    /** روز صفر = ۲۰۲۵-۰۱-۰۱ (شماره‌ی روز از ۱۹۷۰) */
    public static final long BASE_EPOCH_DAY = 20089L;

    /** لایسنس ماهانه (۳۰ روز) — عدد ۰ چون نوع ۳ بیتی است و ۱ تا ۷ پر شده */
    public static final int TYPE_1M = 0;
    public static final int TYPE_1Y = 1;
    public static final int TYPE_2Y = 2;
    public static final int TYPE_3Y = 3;
    /** کد جابجایی دستگاه: تاریخ پایان ثابت (روز صدور + extra) */
    public static final int TYPE_TRANSFER = 4;
    /** لایسنس ۶ ماهه (۱۸۳ روز) */
    public static final int TYPE_6M = 5;
    /** لایسنس ۵ ساله (۱۸۲۵ روز) */
    public static final int TYPE_5Y = 6;

    /** کد آزمایشی: extra = تعداد روز؛ تاریخ پایان ثابت است (روز صدور + extra − ۱) و با وارد کردن دوباره تمدید نمی‌شود */
    public static final int TYPE_TRIAL = 7;

    public static final int DAYS_PER_YEAR = 365;
    public static final int DAYS_6M = 183;
    public static final int DAYS_1M = 30;
    public static final int DAYS_3M = 91;

    /** بیت ۱۰۲۴ از «مقدار اضافه»: لایسنس مخصوص نسخه‌ی نوبت‌دهی پیامکی (کدهای قبلی این بیت را ندارند = عادی) */
    public static final int SMS_FLAG = 1024;
    /** برای نوع ماهانه، «مقدار اضافه» تعداد ماه است (۰ یا ۱ = یک ماه، ۳ = سه ماه) */
    public static final int EXTRA_MASK = 1023;

    // ----- شناسه‌ی «طرح» در برنامه‌ی مدیریتی (فقط مجازی؛ در خود کد ذخیره نمی‌شود) -----
    // پایین‌ترین ۳ بیت = نوع واقعی؛ بیت ۸ = ۳ ماهه (روی نوع ماهانه)؛ بیت ۱۶ = نسخه‌ی پیامکی
    public static final int PLAN_3M = 8;
    public static final int PLAN_SMS = 16;

    public static int realType(int plan) {
        return plan & 7;
    }

    public static boolean isSmsPlan(int plan) {
        return (plan & PLAN_SMS) != 0;
    }

    public static boolean isThreeMonthPlan(int plan) {
        return (plan & PLAN_3M) != 0 && (plan & 7) == TYPE_1M;
    }

    /** مقدار «extra» که برای ساخت کد این طرح لازم است */
    public static int planExtra(int plan) {
        return (isThreeMonthPlan(plan) ? 3 : 0) | (isSmsPlan(plan) ? SMS_FLAG : 0);
    }

    /** تعداد روز یک نوع ماهانه با «تعداد ماه» داده‌شده */
    public static int daysForMonths(int months) {
        if (months <= 1) return DAYS_1M;
        if (months == 3) return DAYS_3M;
        return months * 30;
    }

    /** تعداد روز اعتبار یک نوع لایسنس (۰ برای جابجایی یا نوع ناشناخته) */
    public static int daysForType(int plan) {
        int type = plan & 7;
        switch (type) {
            case TYPE_1M:
                return (plan & PLAN_3M) != 0 ? DAYS_3M : DAYS_1M;
            case TYPE_6M:
                return DAYS_6M;
            case TYPE_1Y:
            case TYPE_2Y:
            case TYPE_3Y:
                return DAYS_PER_YEAR * type;
            case TYPE_5Y:
                return DAYS_PER_YEAR * 5;
            default:
                return 0;
        }
    }

    /** مدت نوع لایسنس به ماه (۰ برای جابجایی/ناشناخته) */
    public static int monthsForType(int plan) {
        int type = plan & 7;
        switch (type) {
            case TYPE_1M:
                return (plan & PLAN_3M) != 0 ? 3 : 1;
            case TYPE_6M:
                return 6;
            case TYPE_1Y:
            case TYPE_2Y:
            case TYPE_3Y:
                return 12 * type;
            case TYPE_5Y:
                return 60;
            default:
                return 0;
        }
    }

    public static final class Decoded {
        public int type;
        public int issueDay;
        /** مقدار اضافه بدون بیت پرچم نسخه */
        public int extra;
        /** کد برای نسخه‌ی نوبت‌دهی پیامکی صادر شده است */
        public boolean sms;

        public long issueEpochDay() {
            return BASE_EPOCH_DAY + issueDay;
        }

        /** تعداد روز اعتبار این کد (۰ اگر نوع کد لایسنس زمان‌دار نیست) */
        public int days() {
            if (type == TYPE_1M) return daysForMonths(extra);
            return daysForType(type);
        }

        /** تعداد ماه برای نوع ماهانه (۱ یا ۳) */
        public int months() {
            return type == TYPE_1M ? Math.max(1, extra) : 0;
        }

        public boolean isTransfer() {
            return type == TYPE_TRANSFER;
        }

        public boolean isTrial() {
            return type == TYPE_TRIAL;
        }
    }

    private LicenseCodec() {
    }

    // ---------------- نرمال‌سازی ورودی ----------------

    /** حروف کوچک، خط تیره، فاصله و ارقام فارسی را درست می‌کند؛ O←0 و I/L←1 */
    private static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') c = (char) ('0' + (c - '\u06F0'));
            else if (c >= '\u0660' && c <= '\u0669') c = (char) ('0' + (c - '\u0660'));
            c = Character.toUpperCase(c);
            if (c == 'O') c = '0';
            else if (c == 'I' || c == 'L') c = '1';
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')) sb.append(c);
        }
        return sb.toString();
    }

    public static String normalizeCode(String raw) {
        return clean(raw);
    }

    public static String normalizeDevice(String raw) {
        return clean(raw);
    }

    /** XXXX-XXXX-XXXX */
    public static String formatCode(String code12) {
        String c = clean(code12);
        if (c.length() != 12) return c;
        return c.substring(0, 4) + "-" + c.substring(4, 8) + "-" + c.substring(8, 12);
    }

    // ---------------- شناسه‌ی دستگاه ----------------

    /** شناسه‌ی کوتاه و خوانا (XXXXX-XXXXX) از روی یک مقدار پایدار دستگاه؛ خود مقدار اصلی فاش نمی‌شود. */
    public static String deviceIdFromSeed(String seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("BL-DEV|" + seed).getBytes(StandardCharsets.UTF_8));
            long v = 0;
            for (int i = 0; i < 7; i++) v = (v << 8) | (h[i] & 0xFF);
            v &= (1L << 50) - 1;
            StringBuilder sb = new StringBuilder();
            for (int i = 9; i >= 0; i--) sb.append(ALPHABET.charAt((int) ((v >>> (5 * i)) & 31)));
            return sb.substring(0, 5) + "-" + sb.substring(5, 10);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** امضای کوتاه (۱۶ کاراکتر hex) برای محافظت از مقدارهای ذخیره‌شده‌ی لایسنس در برابر دستکاری */
    public static String signature(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(("BL-SIG|" + data).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i] & 0xFF));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------- ساخت و رمزگشایی ----------------

    private static long mac32(String secret, String deviceId, long payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(("BL1|" + deviceId + "|" + payload).getBytes(StandardCharsets.UTF_8));
            return ((long) (h[0] & 0xFF) << 24) | ((long) (h[1] & 0xFF) << 16)
                    | ((long) (h[2] & 0xFF) << 8) | (h[3] & 0xFF);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** کد ۱۲ کاراکتری (بدون خط تیره) برای دستگاه مشخص */
    public static String generate(String secret, String deviceId, int type, int issueDay, int extra) {
        if (type < 0 || type > 7) throw new IllegalArgumentException("type");
        if (issueDay < 0 || issueDay > 16383) throw new IllegalArgumentException("issueDay");
        if (extra < 0 || extra > 2047) throw new IllegalArgumentException("extra");
        String dev = normalizeDevice(deviceId);
        long payload = ((long) type << 25) | ((long) issueDay << 11) | extra;
        long value = (payload << 32) | mac32(secret, dev, payload);
        StringBuilder sb = new StringBuilder();
        for (int i = 11; i >= 0; i--) sb.append(ALPHABET.charAt((int) ((value >>> (5 * i)) & 31)));
        return sb.toString();
    }

    /** اگر کد برای این دستگاه و با همین کلید ساخته شده باشد مقادیرش را برمی‌گرداند، وگرنه null */
    public static Decoded decode(String secret, String deviceId, String rawCode) {
        String code = clean(rawCode);
        if (code.length() != 12) return null;
        long value = 0;
        for (int i = 0; i < 12; i++) {
            int idx = ALPHABET.indexOf(code.charAt(i));
            if (idx < 0) return null;
            value = (value << 5) | idx;
        }
        long payload = value >>> 32;
        long mac = value & 0xFFFFFFFFL;
        if (mac != mac32(secret, normalizeDevice(deviceId), payload)) return null;
        Decoded d = new Decoded();
        d.type = (int) ((payload >>> 25) & 7);
        d.issueDay = (int) ((payload >>> 11) & 16383);
        int rawExtra = (int) (payload & 2047);
        d.sms = (rawExtra & SMS_FLAG) != 0;
        d.extra = rawExtra & EXTRA_MASK;
        return d;
    }
}
