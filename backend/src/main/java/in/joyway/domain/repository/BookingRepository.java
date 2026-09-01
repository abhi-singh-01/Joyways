package in.joyway.domain.repository;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingReference = :reference")
    Optional<Booking> findByBookingReferenceForUpdate(String reference);

    List<Booking> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.travelPackage
            JOIN FETCH b.customer
            WHERE b.status = in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.travelPackage.startDate = :targetDate
            """)
    List<Booking> findConfirmedBookingsStartingOn(LocalDate targetDate);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.customer
            JOIN FETCH b.travelPackage
            WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithDetails(Long id);

    boolean existsByBookingReference(String bookingReference);

    Optional<Booking> findFirstByBookingReferenceStartingWithOrderByBookingReferenceDesc(String prefix);

    long countByStatus(BookingStatus status);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.paymentStatus = 'PAID'")
    BigDecimal sumPaidRevenue();

    /**
     * Money was captured but the booking never reached CONFIRMED. Usually means a
     * webhook was missed, or payment landed on a booking that had been cancelled.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PAID
            AND b.status <> in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.status <> in.joyway.domain.enums.BookingStatus.COMPLETED
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findPaidButNotConfirmed();

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PAID
            AND b.status <> in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.status <> in.joyway.domain.enums.BookingStatus.COMPLETED
            """)
    long countPaidButNotConfirmed();

    /**
     * The booking is CONFIRMED without a settled payment, meaning seats are held for
     * money that was never captured.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.paymentStatus <> in.joyway.domain.enums.PaymentStatus.PAID
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findConfirmedButNotPaid();

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.status = in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.paymentStatus <> in.joyway.domain.enums.PaymentStatus.PAID
            """)
    long countConfirmedButNotPaid();

    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = in.joyway.domain.enums.BookingStatus.PENDING_PAYMENT
            AND b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PENDING
            AND b.createdAt < :cutoff
            ORDER BY b.createdAt ASC
            """)
    List<Booking> findStalePendingPaymentBookings(Instant cutoff);
}
