package in.joyway.service.operations;

import in.joyway.domain.enums.TripStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class TripStatusTransitions {

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED = new EnumMap<>(TripStatus.class);

    static {
        ALLOWED.put(TripStatus.PLANNED, EnumSet.of(TripStatus.ASSIGNED, TripStatus.CANCELLED));
        ALLOWED.put(TripStatus.ASSIGNED, EnumSet.of(TripStatus.READY, TripStatus.CANCELLED));
        ALLOWED.put(TripStatus.READY, EnumSet.of(TripStatus.IN_PROGRESS, TripStatus.CANCELLED));
        ALLOWED.put(TripStatus.IN_PROGRESS, EnumSet.of(TripStatus.COMPLETED));
        ALLOWED.put(TripStatus.COMPLETED, EnumSet.noneOf(TripStatus.class));
        ALLOWED.put(TripStatus.CANCELLED, EnumSet.noneOf(TripStatus.class));
    }

    private TripStatusTransitions() {
    }

    public static void validate(TripStatus from, TripStatus to) {
        if (from == to) {
            return;
        }
        Set<TripStatus> allowed = ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalArgumentException("Invalid trip status transition from " + from + " to " + to + ".");
        }
    }
}
