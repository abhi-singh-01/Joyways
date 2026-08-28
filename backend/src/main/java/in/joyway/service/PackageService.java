package in.joyway.service;

import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.enums.PackageStatus;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.package_.PackageDetailDto;
import in.joyway.dto.package_.PackageSummaryDto;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.mapper.PackageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageService {

    private final TravelPackageRepository packageRepository;
    private final PackageMapper packageMapper;

    public List<PackageSummaryDto> getPublishedPackages() {
        return packageRepository.findPublishedWithImages(PackageStatus.PUBLISHED)
                .stream()
                .map(packageMapper::toSummaryDto)
                .toList();
    }

    public PackageDetailDto getPublishedPackageBySlug(String slug) {
        TravelPackage pkg = packageRepository.findBySlugWithImages(slug)
                .filter(p -> p.getStatus() == PackageStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));
        return packageMapper.toDetailDto(pkg);
    }

    public PackageDetailDto getPublishedPackageById(Long id) {
        TravelPackage pkg = packageRepository.findByIdWithImages(id)
                .filter(p -> p.getStatus() == PackageStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found or unavailable."));
        return packageMapper.toDetailDto(pkg);
    }
}
