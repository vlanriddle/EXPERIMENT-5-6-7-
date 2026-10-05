package com.example.social.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize,
        boolean last) {

    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize(), page.isLast());
    }
}
