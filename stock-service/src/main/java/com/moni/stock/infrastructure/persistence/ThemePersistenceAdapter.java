package com.moni.stock.infrastructure.persistence;

import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.repository.ThemeRepository;
import com.moni.stock.infrastructure.persistence.entity.ThemeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThemePersistenceAdapter implements ThemeRepository {

    private final ThemeJpaRepository themeJpaRepository;


    @Override
    public void saveAll(List<Theme> themes) {
// Theme 도메인 리스트를 ThemeEntity 리스트로 변환
        List<ThemeEntity> entities = themes.stream()
                .map(theme -> ThemeEntity.builder()
                        .themeName(theme.getThemeName())
                        .themeCode(theme.getThemeCode())
                        .build())
                .toList();

        themeJpaRepository.saveAll(entities);
    }

    @Override
    public List<Theme> findAll() {
        return themeJpaRepository.findAll().stream()
                .map(entity -> Theme.builder()
                        .id(entity.getId())
                        .themeCode(entity.getThemeCode())
                        .themeName(entity.getThemeName())
                        .build())
                .toList();
    }

}
