package com.moni.stock.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Theme {

    private UUID id;
    private String themeCode;
    private String themeName;
}