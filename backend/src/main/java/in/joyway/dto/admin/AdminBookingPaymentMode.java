package in.joyway.dto.admin;

public enum AdminBookingPaymentMode {
    /** Seats held; customer pays later via booking lookup. */
    PENDING_ONLINE,
    /** Cash / UPI / bank already collected; booking is confirmed. */
    PAID_OFFLINE
}
