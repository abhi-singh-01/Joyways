package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.entity.WaitlistEntry;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.domain.repository.WaitlistEntryRepository;
import in.joyway.dto.waitlist.WaitlistJoinRequest;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.notification.BookingNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock private TravelPackageRepository packageRepository;
    @Mock private WaitlistEntryRepository waitlistEntryRepository;
    @Mock private BookingNotificationService bookingNotificationService;

    @InjectMocks
    private WaitlistService waitlistService;

    private TravelPackage pkg;

    @BeforeEach
    void setUp() {
        pkg = TravelPackage.builder()
                .id(3L)
                .slug("nainital-weekend")
                .title("Nainital Weekend")
                .destination("Nainital")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .availableSeats(0)
                .status(PackageStatus.PUBLISHED)
                .build();
    }

    @Test
    void join_rejectsWhenSeatsRemain() {
        pkg.setAvailableSeats(2);
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));

        WaitlistJoinRequest payload = request();
        assertThatThrownBy(() -> waitlistService.join("nainital-weekend", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("still available");
    }

    @Test
    void join_savesNewEntry() {
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndEmail(3L, "priya@example.com"))
                .thenReturn(Optional.empty());
        when(waitlistEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = waitlistService.join("nainital-weekend", request());

        assertThat(response.alreadyJoined()).isFalse();
        ArgumentCaptor<WaitlistEntry> captor = ArgumentCaptor.forClass(WaitlistEntry.class);
        verify(waitlistEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("priya@example.com");
        assertThat(captor.getValue().getSeatsRequested()).isEqualTo(2);
    }

    @Test
    void notifyIfSeatsAvailable_skipsWhenStillFull() {
        when(packageRepository.findById(3L)).thenReturn(Optional.of(pkg));

        assertThat(waitlistService.notifyIfSeatsAvailable(3L)).isZero();
        verify(bookingNotificationService, never()).onWaitlistSeatAvailable(any(), any());
    }

    @Test
    void notifyIfSeatsAvailable_emailsWaitingGuests() {
        pkg.setAvailableSeats(2);
        WaitlistEntry waiting = WaitlistEntry.builder()
                .id(9L)
                .travelPackage(pkg)
                .fullName("Priya")
                .email("priya@example.com")
                .seatsRequested(1)
                .build();
        when(packageRepository.findById(3L)).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndNotifiedAtIsNullOrderByCreatedAtAsc(3L))
                .thenReturn(List.of(waiting));

        assertThat(waitlistService.notifyIfSeatsAvailable(3L)).isEqualTo(1);
        verify(bookingNotificationService).onWaitlistSeatAvailable(pkg, waiting);
        assertThat(waiting.getNotifiedAt()).isNotNull();
    }

    @Test
    void join_rejectsUnpublishedOrPastTrips() {
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.empty());
        WaitlistJoinRequest payload = request();
        assertThatThrownBy(() -> waitlistService.join("nainital-weekend", payload))
                .isInstanceOf(ResourceNotFoundException.class);

        pkg.setStartDate(LocalDate.now().minusDays(1));
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        assertThatThrownBy(() -> waitlistService.join("nainital-weekend", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passed");
    }

    @Test
    void join_returnsAlreadyJoinedWhenStillWaiting() {
        WaitlistEntry existing = WaitlistEntry.builder()
                .id(9L)
                .travelPackage(pkg)
                .fullName("Priya")
                .email("priya@example.com")
                .seatsRequested(1)
                .build();
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndEmail(3L, "priya@example.com"))
                .thenReturn(Optional.of(existing));

        var response = waitlistService.join("nainital-weekend",
                new WaitlistJoinRequest("Priya Sharma", "priya@example.com", "  ", null));

        assertThat(response.alreadyJoined()).isTrue();
        verify(waitlistEntryRepository, never()).save(any());
    }

    @Test
    void notifyIfSeatsAvailable_skipsMissingPackageOrEmptyQueue() {
        when(packageRepository.findById(3L)).thenReturn(Optional.empty());
        assertThat(waitlistService.notifyIfSeatsAvailable(3L)).isZero();

        pkg.setAvailableSeats(2);
        when(packageRepository.findById(3L)).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndNotifiedAtIsNullOrderByCreatedAtAsc(3L))
                .thenReturn(List.of());
        assertThat(waitlistService.notifyIfSeatsAvailable(3L)).isZero();
    }

    @Test
    void notifyIfSeatsAvailable_stillMarksNotifiedWhenEmailFails() {
        pkg.setAvailableSeats(1);
        WaitlistEntry waiting = WaitlistEntry.builder()
                .id(9L)
                .travelPackage(pkg)
                .fullName("Priya")
                .email("priya@example.com")
                .seatsRequested(1)
                .build();
        when(packageRepository.findById(3L)).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndNotifiedAtIsNullOrderByCreatedAtAsc(3L))
                .thenReturn(List.of(waiting));
        doThrow(new RuntimeException("smtp")).when(bookingNotificationService)
                .onWaitlistSeatAvailable(pkg, waiting);

        assertThat(waitlistService.notifyIfSeatsAvailable(3L)).isEqualTo(1);
        assertThat(waiting.getNotifiedAt()).isNotNull();
        verify(waitlistEntryRepository).saveAll(List.of(waiting));
    }

    @Test
    void join_rejoinsAfterPreviousNotification() {
        WaitlistEntry existing = WaitlistEntry.builder()
                .id(9L)
                .travelPackage(pkg)
                .fullName("Priya")
                .email("priya@example.com")
                .notifiedAt(Instant.now())
                .seatsRequested(1)
                .build();
        when(packageRepository.findBySlug("nainital-weekend")).thenReturn(Optional.of(pkg));
        when(waitlistEntryRepository.findByTravelPackageIdAndEmail(3L, "priya@example.com"))
                .thenReturn(Optional.of(existing));
        when(waitlistEntryRepository.save(existing)).thenReturn(existing);

        var response = waitlistService.join("nainital-weekend", request());

        assertThat(response.alreadyJoined()).isFalse();
        assertThat(existing.getNotifiedAt()).isNull();
        assertThat(existing.getSeatsRequested()).isEqualTo(2);
    }

    private static WaitlistJoinRequest request() {
        return new WaitlistJoinRequest("Priya Sharma", "priya@example.com", "9876543210", 2);
    }
}
