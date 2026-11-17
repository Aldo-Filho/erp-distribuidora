package com.pi.erp.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class ApiPagination {

    private ApiPagination() {}

    public static Pageable pageable(int page, int size) {
        return pageable(page, size, Sort.by("id").ascending());
    }

    public static Pageable pageable(int page, int size, Sort sort) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                "Page must be between 0 and 100000 and size between 1 and 100."
            );
        }
        return PageRequest.of(page, size, sort);
    }
}
