package com.moni.admin.infrastructure.client.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int number;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;

    public boolean hasPrevious() {
        return !first;
    }

    public boolean hasNext() {
        return !last;
    }
}
