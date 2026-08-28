package in.joyway.util;

import in.joyway.domain.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TripReferenceGenerator {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^JW-TRIP-(\\d{4})-(\\d{4})$");

    private final TripRepository tripRepository;

    public synchronized String generateNext() {
        int year = Year.now().getValue();
        String prefix = "JW-TRIP-" + year + "-";

        int maxSequence = tripRepository
                .findFirstByTripReferenceStartingWithOrderByTripReferenceDesc(prefix)
                .map(t -> extractSequence(t.getTripReference()))
                .orElse(0);

        for (int attempt = 0; attempt < 100; attempt++) {
            int next = maxSequence + 1 + attempt;
            String reference = prefix + String.format("%04d", next);
            if (!tripRepository.existsByTripReference(reference)) {
                return reference;
            }
        }

        throw new IllegalStateException("Unable to generate unique trip reference.");
    }

    private int extractSequence(String reference) {
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        return 0;
    }
}
