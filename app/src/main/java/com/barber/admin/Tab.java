package com.barber.admin;

import android.view.View;

/** هر تب برنامه: یک نما دارد و می‌تواند داده‌هایش را دوباره از دیتابیس بخواند. */
public interface Tab {
    View view();

    void refresh();
}
