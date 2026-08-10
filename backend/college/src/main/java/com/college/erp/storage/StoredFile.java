package com.college.erp.storage;

import lombok.Builder;
import lombok.Getter;

/** 08-system-architecture.md's file tree: "Response: fileId, url, size". */
@Getter
@Builder
public class StoredFile {

    private String fileId;
    private String url;
    private long size;
}
