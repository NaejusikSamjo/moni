package com.moni.user.user.presentation.dto.response;

import com.moni.user.user.domain.entity.Watchlist;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class WatchlistResponse {

    private UUID id;
    private String stockCode;

    public static WatchlistResponse from(Watchlist watchlist) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .stockCode(watchlist.getStockCode())
                .build();
    }

    public static List<WatchlistResponse> fromList(List<Watchlist> watchlist) {
        return watchlist.stream()
                .map(WatchlistResponse::from)
                .toList();
    }
}