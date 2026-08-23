package com.ebookstore.common.dto;

import java.util.List;

/**
 * Generic paged response envelope.
 * Replaces Spring's default {@code Page} serialisation with the documented shape:
 * <pre>
 * {
 *   "content": [...],
 *   "page": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 }
 * }
 * </pre>
 *
 * @param <T> the content item type
 */
public class PagedResponse<T> {

    private final List<T> content;
    private final PageMetadata page;

    public PagedResponse(List<T> content, PageMetadata page) {
        this.content = content;
        this.page = page;
    }

    /** Factory helper — wraps a Spring {@code Page<T>}. */
    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<T> springPage) {
        PageMetadata meta = new PageMetadata(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages());
        return new PagedResponse<>(springPage.getContent(), meta);
    }

    public List<T> getContent() { return content; }
    public PageMetadata getPage() { return page; }
}
