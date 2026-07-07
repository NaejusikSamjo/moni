package com.moni.ai.presentation.dto.response;

import com.moni.common.response.paging.PageRes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsPageCacheDto {

    private List<NewsResDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static NewsPageCacheDto from(PageRes<NewsResDto> pageRes) {
        return NewsPageCacheDto.builder()
                .content(pageRes.getContent())
                .pageNumber(pageRes.getPageNumber())
                .pageSize(pageRes.getPageSize())
                .totalElements(pageRes.getTotalElements())
                .totalPages(pageRes.getTotalPages())
                .last(pageRes.isLast())
                .build();
    }

    public PageRes<NewsResDto> toPageRes(Pageable pageable) {
        return new PageRes<>(
                new PageImpl<>(content, pageable, totalElements)
        );
    }
}
