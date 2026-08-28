package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.admin.*;
import in.joyway.dto.package_.PackageImageDto;
import in.joyway.service.AdminPackageImageService;
import in.joyway.service.AdminPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/packages")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPackageController {

    private final AdminPackageService packageService;
    private final AdminPackageImageService imageService;

    @GetMapping
    public ApiResponse<List<AdminPackageSummaryDto>> listPackages() {
        return ApiResponse.success(packageService.listAllPackages());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminPackageDetailDto> getPackage(@PathVariable Long id) {
        return ApiResponse.success(packageService.getPackageById(id));
    }

    @PostMapping
    public ApiResponse<AdminPackageDetailDto> createPackage(
            @Valid @RequestBody CreatePackageRequest request
    ) {
        return ApiResponse.success(packageService.createPackage(request), "Package created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminPackageDetailDto> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageRequest request
    ) {
        return ApiResponse.success(packageService.updatePackage(id, request), "Package updated.");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AdminPackageDetailDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageStatusRequest request
    ) {
        return ApiResponse.success(
                packageService.updateStatus(id, request),
                "Package status updated."
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ApiResponse.success(null, "Package deleted.");
    }

    // --- Image management ---

    @GetMapping("/{id}/images")
    public ApiResponse<List<PackageImageDto>> listImages(@PathVariable Long id) {
        return ApiResponse.success(imageService.listImages(id));
    }

    @PostMapping("/{id}/images/url")
    public ApiResponse<PackageImageDto> addImageByUrl(
            @PathVariable Long id,
            @Valid @RequestBody AddPackageImageUrlRequest request
    ) {
        return ApiResponse.success(imageService.addImageByUrl(id, request), "Image added.");
    }

    @PostMapping(value = "/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PackageImageDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText
    ) throws IOException {
        return ApiResponse.success(imageService.uploadImage(id, file, altText), "Image uploaded.");
    }

    @PatchMapping("/{id}/images/{imageId}")
    public ApiResponse<PackageImageDto> updateImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @Valid @RequestBody UpdatePackageImageRequest request
    ) {
        return ApiResponse.success(imageService.updateImage(id, imageId, request), "Image updated.");
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) throws IOException {
        imageService.deleteImage(id, imageId);
        return ApiResponse.success(null, "Image deleted.");
    }
}
