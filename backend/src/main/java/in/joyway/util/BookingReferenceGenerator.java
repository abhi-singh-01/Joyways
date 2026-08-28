package in.joyway.util;

import in.joyway.domain.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class BookingReferenceGenerator {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^JW-(\\d{4})-(\\d{5})$");

    private final BookingRepository bookingRepository;

    public synchronized String generateNext() {
        int year = Year.now().getValue();
        String prefix = "JW-" + year + "-";

        int maxSequence = bookingRepository
                .findFirstByBookingReferenceStartingWithOrderByBookingReferenceDesc(prefix)
                .map(b -> extractSequence(b.getBookingReference()))
                .orElse(0);

        for (int attempt = 0; attempt < 100; attempt++) {
            int next = maxSequence + 1 + attempt;
            String reference = prefix + String.format("%05d", next);
            if (!bookingRepository.existsByBookingReference(reference)) {
                return reference;
            }
        }

        throw new IllegalStateException("Unable to generate unique booking reference.");
    }

    private int extractSequence(String reference) {
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        return 0;
    }
}
