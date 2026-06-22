package com.moni.stock.application.port.in;

import com.moni.stock.domain.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ThemeQueryUseCase {

    Page<Theme> getThemeList(Pageable pageable);
}