package com.moni.stock.application.service;

import com.moni.stock.application.port.in.ThemeQueryUseCase;
import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeService implements ThemeQueryUseCase {

    private final ThemeRepository themeRepository;

    @Override
    public Page<Theme> getThemeList(Pageable pageable) {
        return themeRepository.findAll(pageable);
    }
}