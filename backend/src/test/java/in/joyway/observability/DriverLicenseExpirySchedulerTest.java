package in.joyway.observability;

import in.joyway.domain.entity.Driver;
import in.joyway.domain.repository.DriverRepository;
import in.joyway.service.operations.LicenseStatusHelper;
import in.joyway.service.operations.OperationsNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLicenseExpirySchedulerTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private OperationsNotificationService operationsNotificationService;

    @InjectMocks
    private DriverLicenseExpiryScheduler scheduler;

    @Test
    void notifyExpiringLicenses_notifiesExpiredAndSoonSkipsValid() {
        LocalDate today = LicenseStatusHelper.businessToday();
        Driver expired = Driver.builder().id(1L).name("Expired").licenseExpiryDate(today.minusDays(1)).build();
        Driver soon = Driver.builder().id(2L).name("Soon").licenseExpiryDate(today.plusDays(10)).build();
        Driver valid = Driver.builder().id(3L).name("Valid").licenseExpiryDate(today.plusDays(60)).build();
        when(driverRepository.findActiveWithLicenseExpiringBy(today.plusDays(30)))
                .thenReturn(List.of(expired, soon, valid));

        scheduler.notifyExpiringLicenses();

        verify(operationsNotificationService).notifyDriverLicenseExpiring("Expired", 1L);
        verify(operationsNotificationService).notifyDriverLicenseExpiring("Soon", 2L);
        verify(operationsNotificationService, never()).notifyDriverLicenseExpiring("Valid", 3L);
    }
}
