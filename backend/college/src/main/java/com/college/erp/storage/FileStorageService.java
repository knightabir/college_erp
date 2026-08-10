package com.college.erp.storage;

import java.io.InputStream;

/** 06-design-document.md §9.1. Business code depends only on this interface — the active
 *  implementation is selected by `erp.storage.mode` (see LocalFileStorageService javadoc). */
public interface FileStorageService {

    StoredFile store(FileUploadRequest request, StorageScope scope);

    InputStream retrieve(String fileId);

    /** [FileStorageService's own contract]: soft delete — marks/removes access, not necessarily
     *  a guaranteed byte-for-byte wipe. LocalFileStorageService does delete the underlying file;
     *  see its javadoc. */
    void delete(String fileId);

    /** Not in the design doc's minimal interface, but required by FileController to set the
     *  right Content-Type header on retrieval without a separate file-metadata table (none is
     *  documented in 04-database-schema.md) — see LocalFileStorageService javadoc. */
    String getContentType(String fileId);
}
