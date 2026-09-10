package in.joyway.controller;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.dto.review.PackageReviewsResponse;
import in.joyway.dto.waitlist.WaitlistJoinRequest;
import in.joyway.dto.waitlist.WaitlistJoinResponse;
import in.joyway.service.PackageReviewService;
import in.joyway.service.PackageService;
import in.joyway.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;
    private final WaitlistService waitlistService;
    private final PackageReviewService packageReviewService;

    @GetMapping
    public ApiResponse<List<PackageSummaryDto>> listPackages() {
        return ApiResponse.success(packageService.getPublishedPackages());
    }

    @GetMapping("/id/{id}")
    public ApiResponse<PackageDetailDto> getPackageById(@PathVariable Long id) {
        return ApiResponse.success(packageService.getPublishedPackageById(id));
    }

    @GetMapping("/{slug}/reviews")
    public ApiResponse<PackageReviewsResponse> listReviews(@PathVariable String slug) {
        return ApiResponse.success(packageReviewService.listPublishedReviews(slug));
    }

    @PostMapping("/{slug}/waitlist")
    public ApiResponse<WaitlistJoinResponse> joinWaitlist(
            @PathVariable String slug,
            @Valid @RequestBody WaitlistJoinRequest request
    ) {
        WaitlistJoinResponse response = waitlistService.join(slug, request);
        String message = response.alreadyJoined()
                ? "You are already on the waitlist for this trip."
                : "You are on the waitlist. We will email you if a seat opens.";
        return ApiResponse.success(response, message);
    }

    @GetMapping("/{slug}")
    public ApiResponse<PackageDetailDto> getPackage(@PathVariable String slug) {
        return ApiResponse.success(packageService.getPublishedPackageBySlug(slug));
    }
}
