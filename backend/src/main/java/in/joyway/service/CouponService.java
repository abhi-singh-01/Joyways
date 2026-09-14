package in.joyway.service;

import in.joyway.domain.entity.*;
import in.joyway.domain.enums.CouponDiscountType;
import in.joyway.domain.repository.CouponRedemptionRepository;
import in.joyway.domain.repository.CouponRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.*;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.service.audit.AuditLogService;
import in.joyway.service.operations.LicenseStatusHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final TravelPackageRepository packageRepository;
    private final AuditLogService auditLogService;

    public record AppliedCoupon(Coupon coupon, BigDecimal discountAmount, BigDecimal totalAmount) {}

    @Transactional(readOnly = true)
    public PageResponse<CouponSummaryDto> list(
            String query,
            Boolean active,
            Long packageId,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Coupon> result = couponRepository.search(
                blankToEmpty(query),
                active,
                packageId,
                pageable
        );
        return PageResponse.of(result.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public CouponDetailDto get(Long id) {
        return toDetail(requireCoupon(id));
    }

    public CouponDetailDto create(CreateCouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("A coupon with this code already exists.");
        }
        TravelPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found."));
        CouponDiscountType type = parseType(request.getDiscountType());
        validateDiscountValue(type, request.getDiscountValue());
        validateExpiry(request.getExpiresOn());

        Coupon coupon = Coupon.builder()
                .code(code)
                .travelPackage(pkg)
                .discountType(type)
                .discountValue(scale(request.getDiscountValue()))
                .expiresOn(request.getExpiresOn())
                .active(true)
                .build();
        coupon = couponRepository.save(coupon);
        auditLogService.log("COUPON_CREATED", "COUPON", coupon.getId(),
                "Coupon " + code + " created for package " + pkg.getTitle() + ".");
        return toDetail(coupon);
    }

    public CouponDetailDto update(Long id, UpdateCouponRequest request) {
        Coupon coupon = requireCoupon(id);
        TravelPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found."));
        CouponDiscountType type = parseType(request.getDiscountType());
        validateDiscountValue(type, request.getDiscountValue());
        if (request.getExpiresOn() == null) {
            throw new IllegalArgumentException("Expiry date is required.");
        }

        coupon.setTravelPackage(pkg);
        coupon.setDiscountType(type);
        coupon.setDiscountValue(scale(request.getDiscountValue()));
        coupon.setExpiresOn(request.getExpiresOn());
        coupon.setActive(Boolean.TRUE.equals(request.getActive()));
        coupon = couponRepository.save(coupon);
        auditLogService.log("COUPON_UPDATED", "COUPON", coupon.getId(),
                "Coupon " + coupon.getCode() + " updated.");
        return toDetail(coupon);
    }

    /**
     * Validates a coupon for the given package and guest identity, returning the discounted total.
     * One redemption per customer email or mobile for this coupon (package-scoped by coupon itself).
     */
    @Transactional(readOnly = true)
    public AppliedCoupon applyForBooking(
            String rawCode,
            TravelPackage pkg,
            BigDecimal subtotal,
            String email,
            String mobile
    ) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        String code = normalizeCode(rawCode);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code."));

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("This coupon is no longer active.");
        }
        LocalDate today = LicenseStatusHelper.businessToday();
        if (coupon.getExpiresOn().isBefore(today)) {
            throw new IllegalArgumentException("This coupon has expired.");
        }
        if (!coupon.getTravelPackage().getId().equals(pkg.getId())) {
            throw new IllegalArgumentException("This coupon is not valid for the selected trip.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedMobile = mobile.trim();
        if (redemptionRepository.existsForCustomer(coupon.getId(), normalizedEmail, normalizedMobile)) {
            throw new IllegalArgumentException(
                    "You have already used this coupon for this trip. One use per customer.");
        }

        BigDecimal discount = computeDiscount(coupon, subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new AppliedCoupon(coupon, discount, total);
    }

    public void redeem(AppliedCoupon applied, Booking booking, String email, String mobile) {
        if (applied == null) {
            return;
        }
        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(applied.coupon())
                .booking(booking)
                .travelPackage(booking.getTravelPackage())
                .customerEmail(email.trim().toLowerCase())
                .customerMobile(mobile.trim())
                .discountAmount(applied.discountAmount())
                .build();
        try {
            redemptionRepository.saveAndFlush(redemption);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "You have already used this coupon for this trip. One use per customer.");
        }
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENT) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon requireCoupon(Long id) {
        return couponRepository.findByIdWithPackage(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));
    }

    private CouponSummaryDto toSummary(Coupon coupon) {
        LocalDate today = LicenseStatusHelper.businessToday();
        return CouponSummaryDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .packageId(coupon.getTravelPackage().getId())
                .packageTitle(coupon.getTravelPackage().getTitle())
                .destination(coupon.getTravelPackage().getDestination())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .expiresOn(coupon.getExpiresOn())
                .active(coupon.isActive())
                .expired(coupon.getExpiresOn().isBefore(today))
                .redemptionCount(redemptionRepository.countByCouponId(coupon.getId()))
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    private CouponDetailDto toDetail(Coupon coupon) {
        LocalDate today = LicenseStatusHelper.businessToday();
        return CouponDetailDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .packageId(coupon.getTravelPackage().getId())
                .packageTitle(coupon.getTravelPackage().getTitle())
                .destination(coupon.getTravelPackage().getDestination())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .expiresOn(coupon.getExpiresOn())
                .active(coupon.isActive())
                .expired(coupon.getExpiresOn().isBefore(today))
                .redemptionCount(redemptionRepository.countByCouponId(coupon.getId()))
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private static CouponDiscountType parseType(String raw) {
        try {
            return CouponDiscountType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Discount type must be PERCENT or FIXED.");
        }
    }

    private static void validateDiscountValue(CouponDiscountType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero.");
        }
        if (type == CouponDiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percent discount cannot exceed 100.");
        }
    }

    private static void validateExpiry(LocalDate expiresOn) {
        if (expiresOn == null) {
            throw new IllegalArgumentException("Expiry date is required.");
        }
        if (expiresOn.isBefore(LicenseStatusHelper.businessToday())) {
            throw new IllegalArgumentException("Expiry date must be today or later.");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
