package com.ebookstore.common.dto;

/**
 * Pagination metadata envelope used by all paged API responses.
 * Matches the OpenAPI {@code PageMetadata} schema exactly.
 */
public class PageMetadata {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageMetadata(int page, int size, long totalElements, int totalPages) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public int getPage()              { return page; }
    public int getSize()              { return size; }
    public long getTotalElements()    { return totalElements; }
    public int getTotalPages()        { return totalPages; }
}
