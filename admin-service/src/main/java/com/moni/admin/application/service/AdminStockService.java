package com.moni.admin.application.service;

import com.moni.admin.infrastructure.client.StockAdminClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStockService {

    private final StockAdminClient stockAdminClient;

    public void downloadStocks() {
        stockAdminClient.downloadStocks();
    }
}
