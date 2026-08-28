package in.joyway.service;

import in.joyway.domain.enums.BookingStatus;
import in.joyway.domain.enums.DriverStatus;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.enums.PaymentStatus;
import in.joyway.domain.enums.TripStatus;
import in.joyway.domain.enums.VehicleStatus;
import in.joyway.domain.repository.BookingRepository;
import in.joyway.domain.repository.DriverRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.domain.repository.TripRepository;
import in.joyway.domain.repository.VehicleRepository;
import in.joyway.dto.admin.DashboardDto;
import in.joyway.service.operations.AdminTripService;
import in.joyway.service.operations.LicenseStatusHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository packageRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final AdminTripService adminTripService;

    public DashboardDto getDashboard() {
        BigDecimal revenue = bookingRepository.sumPaidRevenue();
        if (revenue == null) {
            revenue = BigDecimal.ZERO;
        }

        LocalDate today = LicenseStatusHelper.businessToday();

        return DashboardDto.builder()
                .totalBookings(bookingRepository.count())
                .confirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED))
                .pendingPayments(bookingRepository.countByPaymentStatus(PaymentStatus.PENDING))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .revenue(revenue)
                .activePackages(packageRepository.countByStatus(PackageStatus.PUBLISHED))
                .upcomingTrips(packageRepository.countUpcomingPublished())
                .availableSeats(packageRepository.sumAvailableSeatsPublished())
                .todaysTrips(tripRepository.countTripsOnDate(today))
                .upcomingOperationalTrips(tripRepository.countUpcoming(today))
                .availableVehicles(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE))
                .assignedVehicles(vehicleRepository.countByStatus(VehicleStatus.ASSIGNED)
                        + vehicleRepository.countByStatus(VehicleStatus.ON_TRIP))
                .availableDrivers(driverRepository.countByStatus(DriverStatus.AVAILABLE))
                .assignedDrivers(driverRepository.countByStatus(DriverStatus.ASSIGNED)
                        + driverRepository.countByStatus(DriverStatus.ON_TRIP))
                .tripsInProgress(tripRepository.countByStatus(TripStatus.IN_PROGRESS))
                .tripsCompletedToday(tripRepository.countCompletedOnDate(today))
                .paidButNotConfirmed(bookingRepository.countPaidButNotConfirmed())
                .confirmedButNotPaid(bookingRepository.countConfirmedButNotPaid())
                .upcomingTripList(adminTripService.listUpcomingTrips(5))
                .build();
    }
}
