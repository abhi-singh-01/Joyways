package in.joyway.service.operations;

import java.time.LocalDate;
import java.time.ZoneId;

public final class LicenseStatusHelper {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int EXPIRING_SOON_DAYS = 30;

    private LicenseStatusHelper() {
    }

    public static String resolve(LocalDate licenseExpiryDate) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (licenseExpiryDate.isBefore(today)) {
            return "LICENSE EXPIRED";
        }
        if (!licenseExpiryDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
            return "LICENSE EXPIRING SOON";
        }
        return "VALID";
    }

    public static boolean isExpired(LocalDate licenseExpiryDate) {
        return licenseExpiryDate.isBefore(LocalDate.now(BUSINESS_ZONE));
    }

    public static LocalDate businessToday() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
