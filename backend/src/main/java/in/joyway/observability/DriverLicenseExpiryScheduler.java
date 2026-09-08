package in.joyway.observability;

import in.joyway.domain.entity.Driver;
import in.joyway.domain.repository.DriverRepository;
import in.joyway.service.operations.LicenseStatusHelper;
import in.joyway.service.operations.OperationsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverLicenseExpiryScheduler {

    private static final int EXPIRING_SOON_DAYS = 30;

    private final DriverRepository driverRepository;
    private final OperationsNotificationService operationsNotificationService;

    @Scheduled(cron = "${joyway.ops.license-expiry-cron:0 0 8 * * *}", zone = "Asia/Kolkata")
    public void notifyExpiringLicenses() {
        LocalDate cutoff = LicenseStatusHelper.businessToday().plusDays(EXPIRING_SOON_DAYS);
        List<Driver> drivers = driverRepository.findActiveWithLicenseExpiringBy(cutoff);
        int notified = 0;
        for (Driver driver : drivers) {
            String status = LicenseStatusHelper.resolve(driver.getLicenseExpiryDate());
            if ("VALID".equals(status)) {
                continue;
            }
            operationsNotificationService.notifyDriverLicenseExpiring(driver.getName(), driver.getId());
            notified++;
        }
        if (notified > 0) {
            log.info("event=driver.license_expiry.scan notified={}", notified);
        }
    }
}
