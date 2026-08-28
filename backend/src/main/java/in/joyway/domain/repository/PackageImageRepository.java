package in.joyway.domain.repository;

import in.joyway.domain.entity.PackageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageImageRepository extends JpaRepository<PackageImage, Long> {

    List<PackageImage> findByTravelPackageIdOrderBySortOrderAsc(Long packageId);

    Optional<PackageImage> findByIdAndTravelPackageId(Long id, Long packageId);
}
