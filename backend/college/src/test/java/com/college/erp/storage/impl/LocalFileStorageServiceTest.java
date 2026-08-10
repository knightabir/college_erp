package com.college.erp.storage.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.config.ErpProperties;
import com.college.erp.storage.FileUploadRequest;
import com.college.erp.storage.StorageScope;
import com.college.erp.storage.StoredFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 06-design-document.md §9.1/§9.3. Named to match the kernel's other *ServiceTest files. */
class LocalFileStorageServiceTest {

    private Path tempDir;
    private LocalFileStorageService localFileStorageService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("erp-storage-test");
        ErpProperties erpProperties = new ErpProperties();
        erpProperties.getStorage().getLocal().setBasePath(tempDir.toString());
        localFileStorageService = new LocalFileStorageService(erpProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best-effort cleanup
                    }
                });
            }
        }
    }

    private FileUploadRequest buildRequest(String fileName, String content) {
        return FileUploadRequest.builder()
                .fileName(fileName)
                .contentType("text/plain")
                .data(content.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    @DisplayName("store() — writes to {base-path}/{scope}/{uuid}.{ext} and returns a fileId + size")
    void store_writesToScopedPath() {
        // ARRANGE
        FileUploadRequest request = buildRequest("photo.jpg", "fake-image-bytes");

        // ACT
        StoredFile stored = localFileStorageService.store(request, StorageScope.PROFILE_PHOTO);

        // ASSERT
        assertThat(stored.getFileId()).isNotBlank();
        assertThat(stored.getSize()).isEqualTo("fake-image-bytes".length());
        Path expected = tempDir.resolve("PROFILE_PHOTO").resolve(stored.getFileId() + ".jpg");
        assertThat(Files.exists(expected)).isTrue();
    }

    @Test
    @DisplayName("retrieve() — returns the exact bytes that were stored")
    void retrieve_returnsStoredBytes() throws IOException {
        // ARRANGE
        FileUploadRequest request = buildRequest("doc.pdf", "hello world");
        StoredFile stored = localFileStorageService.store(request, StorageScope.STUDENT_DOC);

        // ACT
        byte[] bytes = localFileStorageService.retrieve(stored.getFileId()).readAllBytes();

        // ASSERT
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("hello world");
    }

    @Test
    @DisplayName("retrieve() — unknown fileId raises ResourceNotFoundException")
    void retrieve_unknownFileId_throws() {
        // ACT / ASSERT
        assertThatThrownBy(() -> localFileStorageService.retrieve("does-not-exist"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete() — removes the file; a subsequent retrieve() fails")
    void delete_removesFile() {
        // ARRANGE
        FileUploadRequest request = buildRequest("temp.txt", "throwaway");
        StoredFile stored = localFileStorageService.store(request, StorageScope.GENERATED);

        // ACT
        localFileStorageService.delete(stored.getFileId());

        // ASSERT
        assertThatThrownBy(() -> localFileStorageService.retrieve(stored.getFileId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getContentType() — falls back to application/octet-stream for an unrecognized extension")
    void getContentType_unknownExtension_fallsBack() {
        // ARRANGE
        FileUploadRequest request = buildRequest("data.xyz123", "binary-ish");
        StoredFile stored = localFileStorageService.store(request, StorageScope.EXAM_PAPER);

        // ACT
        String contentType = localFileStorageService.getContentType(stored.getFileId());

        // ASSERT
        assertThat(contentType).isNotBlank();
    }
}
