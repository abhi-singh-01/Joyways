package in.joyway.service.storage;

import in.joyway.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * One-off migration from local {@code ./storage} to Azure Blob Storage.
 *
 * <p>Enable explicitly only — never runs on normal startup:
 * {@code SPRING_PROFILES_ACTIVE=prod,storage-migration}
 *
 * <p>Database references are unchanged when blob names match existing storage keys
 * ({@code category/uuid.ext}).
 */
@Component
@Profile("storage-migration")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageMigrationRunner implements CommandLineRunner {

    private final StorageProperties storageProperties;
    private final DocumentStorageService documentStorageService;

    @Override
    public void run(String... args) throws Exception {
        if (!storageProperties.isAzureProvider()) {
            throw new IllegalStateException("Migration requires STORAGE_PROVIDER=azure.");
        }

        Path root = Path.of(storageProperties.getLocalPath()).normalize().toAbsolutePath();
        if (!Files.isDirectory(root)) {
            log.warn("event=storage.migration.skipped reason=local_root_missing path={}", root);
            return;
        }

        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String storageKey = root.relativize(file.normalize()).toString().replace('\\', '/');
                try {
                    StorageKeyValidator.validate(storageKey);
                    if (documentStorageService.exists(storageKey)) {
                        log.info("event=storage.migration.exists key={}", storageKey);
                        succeeded.add(storageKey);
                        return;
                    }
                    migrateFile(storageKey, file);
                    if (documentStorageService.exists(storageKey)) {
                        succeeded.add(storageKey);
                        log.info("event=storage.migration.uploaded key={}", storageKey);
                    } else {
                        failed.add(storageKey);
                    }
                } catch (Exception ex) {
                    failed.add(storageKey);
                    log.warn("event=storage.migration.failed key={} reason={}",
                            storageKey, ex.getClass().getSimpleName());
                }
            });
        }

        log.info("event=storage.migration.complete succeeded={} failed={}", succeeded.size(), failed.size());
        if (!failed.isEmpty()) {
            log.warn("event=storage.migration.failures keys={}", failed);
        }
    }

    private void migrateFile(String storageKey, Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String contentType = StorageContentTypeResolver.fromStorageKey(storageKey);
        documentStorageService.putObject(storageKey, bytes, contentType);
    }
}
