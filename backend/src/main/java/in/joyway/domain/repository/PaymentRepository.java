package in.joyway.domain.repository;

import in.joyway.domain.entity.Payment;
import in.joyway.domain.enums.PaymentRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<Payment> findByBookingIdAndStatus(Long bookingId, PaymentRecordStatus status);
}
