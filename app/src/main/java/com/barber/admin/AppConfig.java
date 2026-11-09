package com.barber.admin;

import android.content.Context;

/** تنظیمات ثابت و ترجیحات برنامه‌ی مدیریتی. */
public final class AppConfig {
    public static final String CURRENCY = "تومان";

    /**
     * کلید مخفی سازنده/بررسی‌کننده‌ی کد لایسنس.
     * ⚠️ دقیقاً همین مقدار باید در «برنامه‌ی آرایشگاه» هم باشد، وگرنه کدها قبول نمی‌شوند.
     * مخزن (Repository) GitHub را حتماً خصوصی (Private) نگه دارید و این مقدار را جایی منتشر نکنید.
     */
    static final String SECRET = "5b0752dcd05010fcfe835076876430e76784702ba4b45b224c5abf1585e1a1e6";

    private AppConfig() {
    }

    // ---------------- نوع رقم‌ها (فارسی/انگلیسی) ----------------

    public static boolean isFaDigits(Context ctx) {
        return ctx.getSharedPreferences("digits_settings", Context.MODE_PRIVATE).getBoolean("fa", true);
    }

    public static void setFaDigits(Context ctx, boolean fa) {
        ctx.getSharedPreferences("digits_settings", Context.MODE_PRIVATE).edit().putBoolean("fa", fa).apply();
    }
}
