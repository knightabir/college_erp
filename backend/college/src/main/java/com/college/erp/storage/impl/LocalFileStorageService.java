package com.college.erp.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.config.ErpProperties;
import com.college.erp.storage.FileStorageService;
import com.college.erp.storage.FileUploadRequest;
import com.college.erp.storage.StorageScope;
import com.college.erp.storage.StoredFile;

import lombok.RequiredArgsConstructor;

/**
 * 06-design-document.md §9.3: active when `erp.storage.mode=local` (the default — air-gap safe,
 * ADR-001). `MinioFileStorageService`/`S3FileStorageService` are NOT implemented — neither the
 * MinIO client nor the AWS S3 SDK is a `pom.xml` dependency, and adding a cloud storage SDK is a
 * bigger scope/dependency decision than this pass takes on (same reasoning as EmailChannel/
 * SmsChannel staying stubs rather than integrating a guessed provider). The interface is written
 * so a future `@ConditionalOnProperty(erp.storage.mode=minio|s3)` implementation slots in without
 * any caller changing.
 *
 * Saves to {base-path}/{scope}/{uuid}.{ext}, matching 08-system-architecture.md §8's layout.
 * fileId returned to callers is JUST the UUID (matching the architecture doc's "Returns: fileId
 * (UUID string)") — there's no file-metadata table anywhere in 04-database-schema.md to record
 * which scope/extension a given UUID belongs to, so retrieve()/delete()/getContentType() locate
 * the file by scanning each StorageScope subdirectory for a name starting with "{fileId}.". That
 * scan is O(number of scopes), not O(files) — cheap at this system's scale.
 */
@Service
@ConditionalOnProperty(prefix = "erp.storage", name = "mode", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final ErpProperties erpProperties;

    @Override
    public StoredFile store(FileUploadRequest request, StorageScope scope) {
        try {
            Path scopeDir = scopeDir(scope);
            Files.createDirectories(scopeDir);

            String fileId = UUID.randomUUID().toString();
            String extension = extensionOf(request.getFileName());
            Path target = scopeDir.resolve(fileId + extension);
            Files.write(target, request.getData());

            return StoredFile.builder()
                    .fileId(fileId)
                    .url("/api/v1/files/" + fileId)
                    .size(request.getData() != null ? request.getData().length : 0)
                    .build();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store file", ex);
        }
    }

    @Override
    public InputStream retrieve(String fileId) {
        Path file = locate(fileId);
        try {
            return Files.newInputStream(file);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read file " + fileId, ex);
        }
    }

    @Override
    public void delete(String fileId) {
        Path file = locate(fileId);
        try {
            Files.delete(file);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete file " + fileId, ex);
        }
    }

    @Override
    public String getContentType(String fileId) {
        Path file = locate(fileId);
        try {
            String probed = Files.probeContentType(file);
            return probed != null ? probed : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    private Path locate(String fileId) {
        Path basePath = Path.of(erpProperties.getStorage().getLocal().getBasePath());
        for (StorageScope scope : StorageScope.values()) {
            Path scopeDir = basePath.resolve(scope.name());
            if (!Files.isDirectory(scopeDir)) {
                continue;
            }
            try (Stream<Path> entries = Files.list(scopeDir)) {
                var match = entries.filter(p -> p.getFileName().toString().startsWith(fileId + "."))
                        .findFirst();
                if (match.isPresent()) {
                    return match.get();
                }
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to scan storage scope " + scope, ex);
            }
        }
        throw new ResourceNotFoundException("File", fileId);
    }

    private Path scopeDir(StorageScope scope) {
        return Path.of(erpProperties.getStorage().getLocal().getBasePath()).resolve(scope.name());
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}
