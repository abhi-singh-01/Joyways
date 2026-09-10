package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.entity.WaitlistEntry;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.domain.repository.WaitlistEntryRepository;
import in.joyway.dto.waitlist.WaitlistJoinRequest;
import in.joyway.dto.waitlist.WaitlistJoinResponse;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.notification.BookingNotificationService;
import in.joyway.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final TravelPackageRepository packageRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final BookingNotificationService bookingNotificationService;

    @Transactional
    public WaitlistJoinResponse join(String slug, WaitlistJoinRequest request) {
        TravelPackage pkg = packageRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == PackageStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));

        if (pkg.getStartDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException("This trip date has passed.");
        }
        if (pkg.getAvailableSeats() > 0) {
            throw new IllegalArgumentException("Seats are still available. Book this trip instead.");
        }

        String email = EmailUtils.normalize(request.email());
        int seats = request.seatsRequested() == null ? 1 : request.seatsRequested();
        String mobile = request.mobile() == null || request.mobile().isBlank()
                ? null
                : request.mobile().trim();

        WaitlistEntry existing = waitlistEntryRepository
                .findByTravelPackageIdAndEmail(pkg.getId(), email)
                .orElse(null);
        if (existing != null) {
            if (existing.getNotifiedAt() == null) {
                return WaitlistJoinResponse.builder()
                        .alreadyJoined(true)
                        .email(email)
                        .build();
            }
            existing.setFullName(request.fullName().trim());
            existing.setMobile(mobile);
            existing.setSeatsRequested(seats);
            existing.setNotifiedAt(null);
            waitlistEntryRepository.save(existing);
            log.info("event=waitlist.rejoined packageId={} email={}", pkg.getId(), email);
            return WaitlistJoinResponse.builder()
                    .alreadyJoined(false)
                    .email(email)
                    .build();
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .travelPackage(pkg)
                .fullName(request.fullName().trim())
                .email(email)
                .mobile(mobile)
                .seatsRequested(seats)
                .build();
        waitlistEntryRepository.save(entry);
        log.info("event=waitlist.joined packageId={} seats={}", pkg.getId(), seats);
        return WaitlistJoinResponse.builder()
                .alreadyJoined(false)
                .email(email)
                .build();
    }

    @Transactional
    public int notifyIfSeatsAvailable(Long packageId) {
        TravelPackage pkg = packageRepository.findById(packageId).orElse(null);
        if (pkg == null || pkg.getAvailableSeats() <= 0) {
            return 0;
        }
        List<WaitlistEntry> waiting = waitlistEntryRepository
                .findByTravelPackageIdAndNotifiedAtIsNullOrderByCreatedAtAsc(packageId);
        if (waiting.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        for (WaitlistEntry entry : waiting) {
            try {
                bookingNotificationService.onWaitlistSeatAvailable(pkg, entry);
            } catch (RuntimeException ex) {
                log.warn("Waitlist notification failed for packageId={} entryId={}: {}",
                        packageId, entry.getId(), ex.getClass().getSimpleName());
            }
            entry.setNotifiedAt(now);
        }
        waitlistEntryRepository.saveAll(waiting);
        log.info("event=waitlist.notified packageId={} count={}", packageId, waiting.size());
        return waiting.size();
    }
}
