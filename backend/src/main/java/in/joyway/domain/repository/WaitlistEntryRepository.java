package in.joyway.domain.repository;

import in.joyway.domain.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByTravelPackageIdAndEmail(Long packageId, String email);

    List<WaitlistEntry> findByTravelPackageIdAndNotifiedAtIsNullOrderByCreatedAtAsc(Long packageId);

    @EntityGraph(attributePaths = "travelPackage")
    @Query("SELECT w FROM WaitlistEntry w WHERE w.id = :id")
    Optional<WaitlistEntry> findByIdWithPackage(Long id);
}
