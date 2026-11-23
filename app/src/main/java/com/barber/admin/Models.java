package com.barber.admin;

/** مدل‌های ساده‌ی داده (فیلد عمومی، بدون منطق). */
public final class Models {

    private Models() {
    }

    public static class Customer {
        public long id;
        public String shopName = "";
        public String ownerName = "";
        public String mobile = "";
        public String phone = "";
        public String city = "";
        public String address = "";
        public String nationalId = "";
        public String economicCode = "";
        public String postalCode = "";
        public long visitorId;        // 0 = بدون ویزیتور
        public String metDate = "";   // yyyy-MM-dd میلادی
        public String notes = "";
        public String createdAt = "";
    }

    /** یک بازدید/دمو؛ status: 1 منتظر جواب، 2 علاقه‌مند، 3 نخواست، 4 مشتری شد */
    public static class Lead {
        public long id;
        public String shopName = "";
        public String ownerName = "";
        public String mobile = "";
        public String city = "";
        public String address = "";
        public String area = "";          // محله
        public String visitDate = "";     // yyyy-MM-dd میلادی
        public long visitorId;            // 0 = خودم
        public int status = 1;
        public String followUpDate = "";  // خالی = بدون پیگیری
        public String declineReason = ""; // فقط وقتی status == 3
        public String notes = "";
        public long customerId;           // اگر به مشتری تبدیل شده
        public String createdAt = "";
    }

    public static class Visitor {
        public long id;
        public String name = "";
        public String mobile = "";
        public double percent;
        public boolean active = true;
        public String notes = "";
        public String areas = "";   // محله‌های مجاز (با ویرگول جدا)؛ خالی = بدون محدودیت
        public String createdAt = "";
    }

    public static class License {
        public long id;
        public long customerId;
        public String deviceId = "";
        public int type;              // نوع کد (LicenseCodec.TYPE_*)
        public String code = "";
        public String issueDate = "";
        public int months;
        public long basePrice;
        public double discountPercent;
        public long finalPrice;
        public long visitorId;
        public double commissionPercent;
        public long commissionAmount;
        public String endDate = "";   // برای کد جابجایی و آزمایشی: تاریخ پایان ثابت (میلادی)
        public boolean voided;
        public String notes = "";
        public String createdAt = "";
    }

    public static class Payment {
        public long id;
        public long customerId;
        public long licenseId;        // 0 = عمومی
        public long amount;
        public String payDate = "";
        public String method = "";
        public String note = "";
    }

    public static class Payout {
        public long id;
        public long visitorId;
        public long licenseId;        // 0 = عمومی
        public long amount;
        public String payDate = "";
        public String note = "";
    }

    public static class PriceRow {
        public int type;
        public long price;
        public double discountPercent;
    }

    /** مانده‌ی حساب: کل، پرداخت‌شده، باقی‌مانده */
    public static class Balance {
        public long total;
        public long paid;

        public long due() {
            return total - paid;
        }
    }
}
