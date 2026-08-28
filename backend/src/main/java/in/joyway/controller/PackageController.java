package in.joyway.controller;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ApiResponse<List<PackageSummaryDto>> listPackages() {
        return ApiResponse.success(packageService.getPublishedPackages());
    }

    @GetMapping("/id/{id}")
    public ApiResponse<PackageDetailDto> getPackageById(@PathVariable Long id) {
        return ApiResponse.success(packageService.getPublishedPackageById(id));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PackageDetailDto> getPackage(@PathVariable String slug) {
        return ApiResponse.success(packageService.getPublishedPackageBySlug(slug));
    }
}
