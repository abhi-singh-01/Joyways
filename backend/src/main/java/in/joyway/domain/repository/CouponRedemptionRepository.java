package in.joyway.domain.repository;

import in.joyway.domain.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    @Query("""
            SELECT COUNT(r) > 0 FROM CouponRedemption r
            WHERE r.coupon.id = :couponId
              AND (lower(r.customerEmail) = lower(:email) OR r.customerMobile = :mobile)
            """)
    boolean existsForCustomer(
            @Param("couponId") Long couponId,
            @Param("email") String email,
            @Param("mobile") String mobile
    );

    long countByCouponId(Long couponId);
}
