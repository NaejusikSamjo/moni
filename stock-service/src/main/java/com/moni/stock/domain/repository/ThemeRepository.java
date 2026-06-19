package com.moni.stock.domain.repository;

import com.moni.stock.domain.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ThemeRepository {

    Page<Theme> findAll(Pageable pageable);
}