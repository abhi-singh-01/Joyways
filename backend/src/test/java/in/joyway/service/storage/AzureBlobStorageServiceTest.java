package in.joyway.service.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import in.joyway.config.StorageProperties;
import in.joyway.exception.StorageOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageServiceTest {

    @Test
    void resolveAccountName_prefersAccessKey() {
        StorageProperties properties = new StorageProperties();
        properties.setAccessKey("joywayprod");
        properties.setEndpoint("https://otheraccount.blob.core.windows.net");

        assertEquals("joywayprod", AzureBlobStorageService.resolveAccountName(properties));
    }

    @Test
    void resolveAccountName_parsesEndpointHost() {
        StorageProperties properties = new StorageProperties();
        properties.setEndpoint("https://joywayprod.blob.core.windows.net");

        assertEquals("joywayprod", AzureBlobStorageService.resolveAccountName(properties));
    }

    @Test
    void requireEndpoint_failsWhenMissing() {
        StorageProperties properties = new StorageProperties();
        assertThrows(IllegalStateException.class, () -> AzureBlobStorageService.requireEndpoint(properties));
    }

    @Test
    void assertContainerAccessible_failsWhenContainerMissing() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(containerClient.exists()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> AzureBlobStorageService.assertContainerAccessible(containerClient, "joyway-documents"));
    }

    @Test
    void retrieve_returnsEmptyWhenBlobMissing() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("package-images/missing.jpg")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);

        assertTrue(service.retrieve("package-images/missing.jpg").isEmpty());
    }

    @Test
    void retrieve_throwsWhenDownloadFails() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("package-images/broken.jpg")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenThrow(new RuntimeException("azure-down"));

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);

        assertThrows(StorageOperationException.class, () -> service.retrieve("package-images/broken.jpg"));
    }

    @Test
    void store_uploadsBlobAndReturnsKey() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient(any())).thenReturn(blobClient);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);
        MockMultipartFile file = jpegFile();

        String key = service.store(file, "package-images");

        assertTrue(key.startsWith("package-images/"));
        assertTrue(key.endsWith(".jpg"));
        verify(blobClient).upload(any(BinaryData.class), eq(true));
        verify(blobClient).setHttpHeaders(any(BlobHttpHeaders.class));
    }

    @Test
    void store_rejectsPathTraversalCategory() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(containerClient.exists()).thenReturn(true);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);

        assertThrows(IllegalArgumentException.class, () -> service.store(jpegFile(), "../secrets"));
    }

    @Test
    void delete_removesBlob() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("package-images/remove.jpg")).thenReturn(blobClient);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);
        service.delete("package-images/remove.jpg");

        verify(blobClient).deleteIfExists();
    }

    @Test
    void exists_delegatesToBlobClient() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.exists()).thenReturn(true);
        when(containerClient.getBlobClient("package-images/present.jpg")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);

        assertTrue(service.exists("package-images/present.jpg"));
    }

    @Test
    void getReadAccessUrl_emptyWhenManagedIdentity() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        when(containerClient.exists()).thenReturn(true);

        AzureBlobStorageService service = new AzureBlobStorageService(managedIdentityProperties(), containerClient);

        Optional<String> url = service.getReadAccessUrl("package-images/present.jpg");
        assertFalse(url.isPresent());
    }

    private static StorageProperties managedIdentityProperties() {
        StorageProperties properties = new StorageProperties();
        properties.setProvider("azure");
        properties.setEndpoint("https://testaccount.blob.core.windows.net");
        properties.setContainer("joyway-documents");
        properties.setUseManagedIdentity(true);
        return properties;
    }

    private static MockMultipartFile jpegFile() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg);
    }
}
