package in.joyway.domain.repository;

import in.joyway.domain.entity.Booking;
import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"customer", "travelPackage"})
    @Query("""
            SELECT b FROM Booking b
            JOIN b.customer c
            JOIN b.travelPackage p
            WHERE (:bookingStatus IS NULL OR b.status = :bookingStatus)
            AND (:paymentStatus IS NULL OR b.paymentStatus = :paymentStatus)
            AND (:destination = '' OR LOWER(p.destination) = LOWER(:destination))
            AND (:query = '' OR LOWER(b.bookingReference) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR LOWER(c.fullName) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR LOWER(c.email) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%')
                 OR LOWER(p.title) LIKE CONCAT('%', LOWER(CAST(:query AS string)), '%'))
            """)
    Page<Booking> search(
            String query,
            BookingStatus bookingStatus,
            PaymentStatus paymentStatus,
            String destination,
            Pageable pageable
    );

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
            AND b.createdByAdmin = false
            AND b.createdAt < :cutoff
            ORDER BY b.createdAt ASC
            """)
    List<Booking> findStalePendingPaymentBookings(Instant cutoff);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.customer
            JOIN FETCH b.travelPackage
            WHERE b.status = in.joyway.domain.enums.BookingStatus.PENDING_PAYMENT
            AND b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PENDING
            AND b.createdByAdmin = false
            AND b.createdAt >= :notYetExpiredAfter
            AND b.createdAt < :reminderBefore
            ORDER BY b.createdAt ASC
            """)
    List<Booking> findPendingPaymentBookingsNearingExpiry(
            Instant notYetExpiredAfter,
            Instant reminderBefore
    );

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.travelPackage
            JOIN FETCH b.customer
            WHERE b.status = in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PAID
            AND b.travelPackage.endDate < :today
            """)
    List<Booking> findConfirmedPaidTripsEndedBefore(LocalDate today);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.travelPackage
            JOIN FETCH b.customer
            WHERE b.travelPackage.id = :packageId
            AND b.status = in.joyway.domain.enums.BookingStatus.CONFIRMED
            AND b.paymentStatus = in.joyway.domain.enums.PaymentStatus.PAID
            """)
    List<Booking> findConfirmedPaidByPackageId(Long packageId);
}
