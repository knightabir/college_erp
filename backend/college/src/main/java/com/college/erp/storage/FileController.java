package com.college.erp.storage;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 05-api-documentation.md §14 "File Access". Only GET is documented — nothing in the API doc
 * exposes a generic upload endpoint (each module that needs to store a file, e.g. student
 * documents/asset photos, is expected to call FileStorageService.store() itself, scoped
 * appropriately — none of those call sites have been wired up to this module yet, see
 * CLAUDE.md's `module.storage` deviations note).
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> retrieve(@PathVariable String fileId) {
        InputStream stream = fileStorageService.retrieve(fileId);
        String contentType = fileStorageService.getContentType(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileId + "\"")
                .body(new InputStreamResource(stream));
    }
}
