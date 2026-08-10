package com.college.erp.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a {@link Pageable} from raw request params. Written once here so no controller/service
 * hand-rolls page/size/sort parsing. See 07-coding-standards.md §1.1.
 */
public final class PaginationUtil {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    private PaginationUtil() {
    }

    public static Pageable of(Integer page, Integer size) {
        return of(page, size, null, null);
    }

    public static Pageable of(Integer page, Integer size, String sortBy, String direction) {
        int safePage = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(safePage, safeSize);
        }

        Sort.Direction dir = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, sortBy));
    }
}
