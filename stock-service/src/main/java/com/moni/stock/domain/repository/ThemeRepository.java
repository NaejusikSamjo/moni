package com.moni.stock.domain.repository;

import com.moni.stock.domain.entity.Theme;
import java.util.List;

public interface ThemeRepository {

    void saveAll(List<Theme> themes);

    List<Theme> findAll();
}