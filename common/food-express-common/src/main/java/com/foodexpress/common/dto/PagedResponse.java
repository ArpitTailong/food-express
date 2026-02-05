package com.foodexpress.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Paginated response wrapper using Java 21 Record.
 * Provides standardized pagination metadata.
 *
 * @param <T> The type of items in the page
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponse<T>(
        List<T> content,
        PageInfo pageInfo,
        SortInfo sortInfo
) {
    
    /**
     * Pagination metadata
     */
    public record PageInfo(
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static PageInfo of(int pageNumber, int pageSize, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            return new PageInfo(
                    pageNumber,
                    pageSize,
                    totalElements,
                    totalPages,
                    pageNumber == 0,
                    pageNumber >= totalPages - 1,
                    pageNumber < totalPages - 1,
                    pageNumber > 0
            );
        }
    }
    
    /**
     * Sorting metadata
     */
    public record SortInfo(
            String sortBy,
            String direction,
            boolean sorted
    ) {
        public static SortInfo unsorted() {
            return new SortInfo(null, null, false);
        }
        
        public static SortInfo of(String sortBy, String direction) {
            return new SortInfo(sortBy, direction, true);
        }
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    /**
     * Create a paged response from list with pagination info
     */
    public static <T> PagedResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        return new PagedResponse<>(
                content,
                PageInfo.of(pageNumber, pageSize, totalElements),
                SortInfo.unsorted()
        );
    }
    
    /**
     * Create a paged response with sorting info
     */
    public static <T> PagedResponse<T> of(List<T> content, int pageNumber, int pageSize, 
                                           long totalElements, String sortBy, String direction) {
        return new PagedResponse<>(
                content,
                PageInfo.of(pageNumber, pageSize, totalElements),
                SortInfo.of(sortBy, direction)
        );
    }
    
    /**
     * Create an empty paged response
     */
    public static <T> PagedResponse<T> empty(int pageSize) {
        return new PagedResponse<>(
                List.of(),
                PageInfo.of(0, pageSize, 0),
                SortInfo.unsorted()
        );
    }
    
    /**
     * Convenience method to check if page has content
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
    
    /**
     * Get the number of elements in this page
     */
    public int numberOfElements() {
        return content != null ? content.size() : 0;
    }
}
