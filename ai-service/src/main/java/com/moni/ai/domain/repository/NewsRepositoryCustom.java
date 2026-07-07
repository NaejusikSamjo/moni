package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.NewsEntity;
import com.moni.ai.presentation.dto.request.NewsSearchReqDto;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

public interface NewsRepositoryCustom {
    Page<NewsEntity> searchNews(NewsSearchReqDto request, Pageable pageable);
}
