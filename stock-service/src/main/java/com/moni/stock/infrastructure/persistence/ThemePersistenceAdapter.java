package com.moni.stock.infrastructure.persistence;

import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.repository.ThemeRepository;
import com.moni.stock.infrastructure.persistence.entity.ThemeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ThemePersistenceAdapter implements ThemeRepository {

    private final ThemeJpaRepository themeJpaRepository;

    @Override
    public Page<Theme> findAll(Pageable pageable) {
        return themeJpaRepository.findAll(pageable).map(ThemeEntity::toDomain);
    }
}