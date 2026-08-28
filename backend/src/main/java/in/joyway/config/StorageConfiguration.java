package in.joyway.config;

import in.joyway.service.storage.AzureBlobStorageService;
import in.joyway.service.storage.DocumentStorageService;
import in.joyway.service.storage.LocalDocumentStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
    DocumentStorageService localDocumentStorageService(StorageProperties properties) {
        return new LocalDocumentStorageService(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "azure")
    DocumentStorageService azureDocumentStorageService(StorageProperties properties) {
        return new AzureBlobStorageService(properties);
    }
}
