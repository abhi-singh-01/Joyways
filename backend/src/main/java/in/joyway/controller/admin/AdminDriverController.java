package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.operations.*;
import in.joyway.service.operations.AdminDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDriverController {

    private final AdminDriverService driverService;

    @GetMapping
    public ApiResponse<PageResponse<DriverSummaryDto>> listDrivers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String licenseFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(driverService.listDrivers(query, status, licenseFilter, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DriverDetailDto> getDriver(@PathVariable Long id) {
        return ApiResponse.success(driverService.getDriver(id));
    }

    @PostMapping
    public ApiResponse<DriverDetailDto> createDriver(
            @Valid @RequestBody CreateDriverRequest request
    ) {
        return ApiResponse.success(driverService.createDriver(request), "Driver created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<DriverDetailDto> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        return ApiResponse.success(driverService.updateDriver(id, request), "Driver updated.");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<DriverDetailDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverStatusRequest request
    ) {
        return ApiResponse.success(driverService.updateStatus(id, request), "Driver status updated.");
    }
}
