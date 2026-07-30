package com.edupaste.payloads;
import lombok.Data;
import java.util.List;

@Data
public class PagedResponse<T> {
    private boolean success = true;
    private List<T> data;
    private String message = "Operation successful";
    private Meta meta;

    @Data
    public static class Meta {
        private int page;
        private int limit;
        private long totalElements;
        private int totalPages;
    }
    
    public PagedResponse(List<T> data, int page, int limit, long totalElements, int totalPages) {
        this.data = data;
        this.meta = new Meta();
        this.meta.setPage(page);
        this.meta.setLimit(limit);
        this.meta.setTotalElements(totalElements);
        this.meta.setTotalPages(totalPages);
    }
}