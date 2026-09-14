package in.joyway.service;

import in.joyway.domain.entity.Coupon;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.CouponDiscountType;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.CouponRedemptionRepository;
import in.joyway.domain.repository.CouponRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.CreateCouponRequest;
import in.joyway.service.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository redemptionRepository;
    @Mock private TravelPackageRepository packageRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CouponService couponService;

    private TravelPackage pkg;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        pkg = TravelPackage.builder()
                .id(7L)
                .title("Delhi Heritage")
                .destination("Delhi")
                .status(PackageStatus.PUBLISHED)
                .build();
        coupon = Coupon.builder()
                .id(1L)
                .code("JOYWAY10")
                .travelPackage(pkg)
                .discountType(CouponDiscountType.PERCENT)
                .discountValue(new BigDecimal("10.00"))
                .expiresOn(LocalDate.now().plusDays(30))
                .active(true)
                .build();
    }

    @Test
    void applyForBooking_appliesPercentDiscountForMatchingPackage() {
        when(couponRepository.findByCodeIgnoreCase("JOYWAY10")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsForCustomer(1L, "guest@example.com", "9876543210"))
                .thenReturn(false);

        CouponService.AppliedCoupon applied = couponService.applyForBooking(
                "joyway10",
                pkg,
                new BigDecimal("1000.00"),
                "guest@example.com",
                "9876543210"
        );

        assertThat(applied).isNotNull();
        assertThat(applied.discountAmount()).isEqualByComparingTo("100.00");
        assertThat(applied.totalAmount()).isEqualByComparingTo("900.00");
    }

    @Test
    void applyForBooking_rejectsWrongPackage() {
        TravelPackage other = TravelPackage.builder().id(99L).title("Other").build();
        when(couponRepository.findByCodeIgnoreCase("JOYWAY10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.applyForBooking(
                "JOYWAY10", other, new BigDecimal("1000.00"), "guest@example.com", "9876543210"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid for the selected trip");
    }

    @Test
    void applyForBooking_rejectsSecondUseBySameCustomer() {
        when(couponRepository.findByCodeIgnoreCase("JOYWAY10")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsForCustomer(1L, "guest@example.com", "9876543210"))
                .thenReturn(true);

        assertThatThrownBy(() -> couponService.applyForBooking(
                "JOYWAY10", pkg, new BigDecimal("1000.00"), "guest@example.com", "9876543210"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void create_normalizesCodeAndPersists() {
        CreateCouponRequest request = new CreateCouponRequest();
        request.setCode(" save10 ");
        request.setPackageId(7L);
        request.setDiscountType("FIXED");
        request.setDiscountValue(new BigDecimal("50"));
        request.setExpiresOn(LocalDate.now().plusDays(7));

        when(couponRepository.existsByCodeIgnoreCase("SAVE10")).thenReturn(false);
        when(packageRepository.findById(7L)).thenReturn(Optional.of(pkg));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> {
            Coupon saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(redemptionRepository.countByCouponId(anyLong())).thenReturn(0L);

        var detail = couponService.create(request);

        assertThat(detail.code()).isEqualTo("SAVE10");
        assertThat(detail.discountType()).isEqualTo("FIXED");
        verify(auditLogService).log(anyString(), anyString(), anyLong(), anyString());
    }
}
