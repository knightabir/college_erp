package com.college.erp.storage;

import lombok.Builder;
import lombok.Getter;

/** 08-system-architecture.md's file tree: "Request: fileName, contentType, data". */
@Getter
@Builder
public class FileUploadRequest {

    private String fileName;
    private String contentType;
    private byte[] data;
}
