package in.joyway.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {

    private final EmailRetryService emailRetryService;

    @Scheduled(fixedDelayString = "${joyway.mail.retry-delay-ms:300000}")
    public void retryFailedEmails() {
        try {
            emailRetryService.retryFailedEmails();
        } catch (Exception ex) {
            log.warn("Email retry scheduler failed: {}", ex.getClass().getSimpleName());
        }
    }
}
