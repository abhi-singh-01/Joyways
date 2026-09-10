package in.joyway.domain.repository;

import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.PaymentRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.booking.id IN :bookingIds
            ORDER BY p.booking.id ASC, p.createdAt DESC
            """)
    List<Payment> findByBookingIdIn(Collection<Long> bookingIds);

    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentRecordStatus status);
}
