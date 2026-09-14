package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.CouponDetailDto;
import in.joyway.dto.admin.CouponSummaryDto;
import in.joyway.dto.admin.CreateCouponRequest;
import in.joyway.dto.admin.UpdateCouponRequest;
import in.joyway.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<PageResponse<CouponSummaryDto>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long packageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(couponService.list(query, active, packageId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CouponDetailDto> get(@PathVariable Long id) {
        return ApiResponse.success(couponService.get(id));
    }

    @PostMapping
    public ApiResponse<CouponDetailDto> create(@Valid @RequestBody CreateCouponRequest request) {
        return ApiResponse.success(couponService.create(request), "Coupon created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponDetailDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request
    ) {
        return ApiResponse.success(couponService.update(id, request), "Coupon updated.");
    }
}
