package in.joyway.service;

import in.joyway.domain.entity.PackageImage;
import in.joyway.domain.entity.TravelPackage;
import in.joyway.domain.repository.PackageImageRepository;
import in.joyway.domain.repository.TravelPackageRepository;
import in.joyway.dto.admin.AddPackageImageUrlRequest;
import in.joyway.dto.admin.UpdatePackageImageRequest;
import in.joyway.dto.package_.PackageImageDto;
import in.joyway.exception.ResourceNotFoundException;
import in.joyway.mapper.AdminMapper;
import in.joyway.service.storage.DocumentStorageService;
import in.joyway.service.storage.StorageUploadValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPackageImageService {

    private static final String PACKAGE_IMAGE_CATEGORY = "package-images";

    private final TravelPackageRepository packageRepository;
    private final PackageImageRepository imageRepository;
    private final DocumentStorageService documentStorageService;
    private final AdminMapper adminMapper;

    @Value("${joyway.public-api-base-url:http://localhost:8080}")
    private String publicApiBaseUrl;

    @Transactional(readOnly = true)
    public List<PackageImageDto> listImages(Long packageId) {
        findPackageOrThrow(packageId);
        return imageRepository.findByTravelPackageIdOrderBySortOrderAsc(packageId)
                .stream()
                .map(img -> adminMapper.toImageDtos(List.of(img)).getFirst())
                .toList();
    }

    @Transactional
    public PackageImageDto addImageByUrl(Long packageId, AddPackageImageUrlRequest request) {
        throw new IllegalArgumentException(
                "Package images must be uploaded as JPEG, JPG, or WebP files under 5MB. Image URLs are not allowed.");
    }

    @Transactional
    public PackageImageDto uploadImage(Long packageId, MultipartFile file, String altText) throws IOException {
        TravelPackage pkg = findPackageOrThrow(packageId);
        StorageUploadValidator.validatePackageImage(file);
        String storageKey = documentStorageService.store(file, PACKAGE_IMAGE_CATEGORY);
        String baseUrl = publicApiBaseUrl.endsWith("/")
                ? publicApiBaseUrl.substring(0, publicApiBaseUrl.length() - 1)
                : publicApiBaseUrl;
        String imageUrl = baseUrl + "/api/files/" + storageKey;

        PackageImage image = PackageImage.builder()
                .travelPackage(pkg)
                .imageUrl(imageUrl)
                .privateStorageKey(storageKey)
                .altText(altText)
                .sortOrder(nextSortOrder(packageId))
                .build();

        image = imageRepository.save(image);
        return adminMapper.toImageDtos(List.of(image)).getFirst();
    }

    @Transactional
    public PackageImageDto updateImage(Long packageId, Long imageId, UpdatePackageImageRequest request) {
        PackageImage image = findImageOrThrow(packageId, imageId);

        if (request.getAltText() != null) {
            image.setAltText(request.getAltText());
        }
        if (request.getSortOrder() != null) {
            image.setSortOrder(request.getSortOrder());
        }

        image = imageRepository.save(image);
        return adminMapper.toImageDtos(List.of(image)).getFirst();
    }

    @Transactional
    public void deleteImage(Long packageId, Long imageId) throws IOException {
        PackageImage image = findImageOrThrow(packageId, imageId);

        if (image.getPrivateStorageKey() != null) {
            documentStorageService.delete(image.getPrivateStorageKey());
        }

        imageRepository.delete(image);
    }

    private TravelPackage findPackageOrThrow(Long packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found."));
    }

    private PackageImage findImageOrThrow(Long packageId, Long imageId) {
        return imageRepository.findByIdAndTravelPackageId(imageId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found."));
    }

    private int nextSortOrder(Long packageId) {
        List<PackageImage> images = imageRepository.findByTravelPackageIdOrderBySortOrderAsc(packageId);
        if (images.isEmpty()) {
            return 0;
        }
        return images.getLast().getSortOrder() + 1;
    }
}
