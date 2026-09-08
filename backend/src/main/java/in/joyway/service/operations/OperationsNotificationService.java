package in.joyway.service.operations;

import in.joyway.domain.enums.NotificationType;
import in.joyway.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationsNotificationService {

    private final NotificationService notificationService;

    public void notifyTripCreated(String tripReference, String destination, Long tripId) {
        notificationService.notifyAdmins(
                NotificationType.TRIP_CREATED,
                "New trip created",
                tripReference + " — " + destination,
                "TRIP",
                tripId
        );
    }

    public void notifyVehicleAssigned(String tripReference, String registration, Long tripId) {
        notificationService.notifyAdmins(
                NotificationType.VEHICLE_ASSIGNED,
                "Vehicle assigned",
                registration + " → " + tripReference,
                "TRIP",
                tripId
        );
    }

    public void notifyDriverAssigned(String tripReference, String driverName, Long tripId) {
        notificationService.notifyAdmins(
                NotificationType.DRIVER_ASSIGNED,
                "Driver assigned",
                driverName + " → " + tripReference,
                "TRIP",
                tripId
        );
    }

    public void notifyTripReady(String tripReference, Long tripId) {
        notificationService.notifyAdmins(
                NotificationType.TRIP_READY,
                "Trip ready",
                tripReference,
                "TRIP",
                tripId
        );
    }

    public void notifyTripCancelled(String tripReference, Long tripId) {
        notificationService.notifyAdmins(
                NotificationType.TRIP_CANCELLED,
                "Trip cancelled",
                tripReference,
                "TRIP",
                tripId
        );
    }

    public void notifyDriverLicenseExpiring(String driverName, Long driverId) {
        notificationService.notifyAdmins(
                NotificationType.DRIVER_LICENSE_EXPIRING,
                "Driver license expiring",
                driverName,
                "DRIVER",
                driverId
        );
    }

    public void notifyDocumentUploaded(String bookingReference, Long bookingId) {
        notificationService.notifyAdmins(
                NotificationType.DOCUMENT_UPLOADED,
                "Traveller documents uploaded",
                bookingReference,
                "BOOKING",
                bookingId
        );
    }

    public void notifyVehicleUnavailable(String registration, String status, Long vehicleId) {
        notificationService.notifyAdmins(
                NotificationType.VEHICLE_UNAVAILABLE,
                "Vehicle unavailable",
                registration + " is now " + status,
                "VEHICLE",
                vehicleId
        );
    }
}
