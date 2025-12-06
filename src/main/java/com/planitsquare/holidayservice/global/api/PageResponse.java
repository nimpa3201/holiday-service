package com.planitsquare.holidayservice.global.api;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;          // 현재 페이지 번호 (0부터 시작)
    private final int size;          // 요청한 페이지 크기
    private final long totalElements; // 전체 요소 수
    private final int totalPages;    // 전체 페이지 수
    private final boolean first;     // 첫 페이지 여부
    private final boolean last;      // 마지막 페이지 여부

    private PageResponse(List<T> content,
                         int page,
                         int size,
                         long totalElements,
                         int totalPages,
                         boolean first,
                         boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}