package in.joyway.controller.admin;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.PageResponse;
import in.joyway.dto.admin.operations.*;
import in.joyway.service.operations.AdminVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vehicles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVehicleController {

    private final AdminVehicleService vehicleService;

    @GetMapping
    public ApiResponse<PageResponse<VehicleSummaryDto>> listVehicles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownershipType,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(vehicleService.listVehicles(
                query, status, ownershipType, vehicleType, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleDetailDto> getVehicle(@PathVariable Long id) {
        return ApiResponse.success(vehicleService.getVehicle(id));
    }

    @PostMapping
    public ApiResponse<VehicleDetailDto> createVehicle(
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        return ApiResponse.success(vehicleService.createVehicle(request), "Vehicle created.");
    }

    @PutMapping("/{id}")
    public ApiResponse<VehicleDetailDto> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        return ApiResponse.success(vehicleService.updateVehicle(id, request), "Vehicle updated.");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<VehicleDetailDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleStatusRequest request
    ) {
        return ApiResponse.success(vehicleService.updateStatus(id, request), "Vehicle status updated.");
    }
}
